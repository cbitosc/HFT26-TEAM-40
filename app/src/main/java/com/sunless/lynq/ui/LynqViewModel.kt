package com.sunless.lynq.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sunless.lynq.LynqApp
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.LynqMessage
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.domain.model.Priority
import com.sunless.lynq.service.LynqMeshService
import com.sunless.lynq.demo.DemoData
import com.sunless.lynq.ai.AiPreferences
import com.sunless.lynq.ai.FeatherlessAiClient
import com.sunless.lynq.ai.ThreadSummary
import com.sunless.lynq.ai.LanguageRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MeshViewMode {
    LIVE,   // Real physical devices discovered & connected over Nearby Connections
    DEMO,   // Simulated 4-node relay with Aarav, Kabir, Meera, Priya
}

class LynqViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LynqApp
    private val database = app.database
    private val router = app.router
    private val crypto = app.cryptoManager
    private val locationProvider = app.locationProvider

    val aiPreferences: AiPreferences = app.aiPreferences
    val aiClient: FeatherlessAiClient = app.aiClient

    val aiApiKey: StateFlow<String> = aiPreferences.apiKey
    val aiModel: StateFlow<String> = aiPreferences.model
    val aiBaseUrl: StateFlow<String> = aiPreferences.baseUrl
    val aiTargetLanguage: StateFlow<String> = aiPreferences.targetLanguage

    private val _threadSummaries = MutableStateFlow<Map<String, ThreadSummary>>(emptyMap())
    val threadSummaries: StateFlow<Map<String, ThreadSummary>> = _threadSummaries.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _messageTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val messageTranslations: StateFlow<Map<String, String>> = _messageTranslations.asStateFlow()

    private val _isTranslatingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isTranslatingMap: StateFlow<Map<String, Boolean>> = _isTranslatingMap.asStateFlow()

    val myDeviceId: String = crypto.deviceId
    private val _myDeviceName = MutableStateFlow(router.myName)
    val myDeviceName: StateFlow<String> = _myDeviceName.asStateFlow()
    val publicKeyString: String = crypto.publicKeyString

    fun updateDeviceName(newName: String) {
        val clean = newName.trim()
        if (clean.isNotBlank()) {
            com.sunless.lynq.util.DeviceNameHelper.setCustomDeviceName(app, clean)
            router.updateMyName(clean)
            _myDeviceName.value = clean
        }
    }

    fun resetDeviceNameToSystem() {
        com.sunless.lynq.util.DeviceNameHelper.setCustomDeviceName(app, "")
        val sysName = com.sunless.lynq.util.DeviceNameHelper.getDeviceName(app)
        router.updateMyName(sysName)
        _myDeviceName.value = sysName
    }

    private val demoPeerIds = setOf("2b7f9c41", "5d80e2aa", "c14b76f3", "8ae0d915")
    private val demoNames = setOf("Aarav", "Meera", "Kabir", "Priya")

    private val _meshMode = MutableStateFlow(MeshViewMode.LIVE)
    val meshMode: StateFlow<MeshViewMode> = _meshMode.asStateFlow()

    fun setMeshMode(mode: MeshViewMode) {
        _meshMode.value = mode
    }

    // Live reactive streams from SQLite DB & Nearby Connections
    val peers: StateFlow<List<Peer>> = database.peersFlow
    val messages: StateFlow<List<LynqMessage>> = database.messagesFlow

    // Live real peers (excluding any residual demo fixtures or unverified phantom entries)
    val livePeers: StateFlow<List<Peer>> = peers.map { list ->
        list.filter {
            it.id !in demoPeerIds &&
            it.name !in demoNames &&
            !it.name.startsWith("Peer-", ignoreCase = true) &&
            it.name != "Peer" &&
            it.id.length > 4 &&
            it.connected
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active peers based on selected MeshViewMode (LIVE or DEMO)
    val activePeers: StateFlow<List<Peer>> = combine(meshMode, livePeers) { mode, realPeers ->
        when (mode) {
            MeshViewMode.LIVE -> realPeers
            MeshViewMode.DEMO -> DemoData.peers
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<LynqMessage>> = messages.map { list ->
        list.filter { it.type == MessageType.SOS }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingMessages: StateFlow<List<LynqMessage>> = messages.map { list ->
        list.filter { it.status == DeliveryStatus.PENDING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reachableCount: StateFlow<Int> = livePeers.map { list ->
        list.count { it.connected }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val relayCount: StateFlow<Int> = livePeers.map { list ->
        list.count { it.relaying }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeReachableCount: StateFlow<Int> = combine(meshMode, reachableCount) { mode, realReachable ->
        when (mode) {
            MeshViewMode.LIVE -> realReachable
            MeshViewMode.DEMO -> DemoData.reachableCount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeRelayCount: StateFlow<Int> = combine(meshMode, relayCount) { mode, realRelay ->
        when (mode) {
            MeshViewMode.LIVE -> realRelay
            MeshViewMode.DEMO -> DemoData.relayCount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isMeshActive = MutableStateFlow(true)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    private val _activeSosBroadcasting = MutableStateFlow(false)
    val activeSosBroadcasting: StateFlow<Boolean> = _activeSosBroadcasting.asStateFlow()

    private val _lastSentSos = MutableStateFlow<LynqMessage?>(null)
    val lastSentSos: StateFlow<LynqMessage?> = _lastSentSos.asStateFlow()

    private val _openPeerId = MutableStateFlow<String?>(null)
    val openPeerId: StateFlow<String?> = _openPeerId.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun openThread(peerId: String?) {
        _openPeerId.value = peerId
    }

    fun clearAlertHistory() {
        viewModelScope.launch {
            database.clearAlertHistory()
            _lastSentSos.value = null
        }
    }

    init {
        // Start mesh routing and foreground service
        startMesh()
        clearAlertHistory()
    }

    fun startMesh() {
        _isMeshActive.value = true
        router.start()
        LynqMeshService.start(getApplication())
    }

    fun stopMesh() {
        _isMeshActive.value = false
        router.stop()
        LynqMeshService.stop(getApplication())
    }

    fun toggleMesh(enabled: Boolean) {
        if (enabled) startMesh() else stopMesh()
    }

    fun sendSos(category: String, note: String, shareLocation: Boolean) {
        viewModelScope.launch {
            _activeSosBroadcasting.value = true
            val coords = if (shareLocation) {
                locationProvider.getEmergencyCoordinates() ?: "28.6139° N, 77.2090° E (±12m)"
            } else {
                null
            }

            val sentMsg = router.sendSos(
                category = category,
                details = note,
                coordinates = coords,
            )
            _lastSentSos.value = sentMsg
        }
    }

    fun triggerSimulation(category: String = "Medical", note: String = "Trapped, need assistance") {
        viewModelScope.launch {
            _meshMode.value = MeshViewMode.DEMO
            _activeSosBroadcasting.value = true
            router.simulateMultiHopDemo(
                originName = "You",
                relayNodes = listOf("Aarav", "Kabir", "Priya"),
                category = category,
                notes = note,
            )
        }
    }

    fun cancelSos() {
        _activeSosBroadcasting.value = false
        _lastSentSos.value = null
    }

    fun sendChatMessage(recipient: Peer, text: String, priority: Priority = Priority.NORMAL) {
        viewModelScope.launch {
            router.sendChatMessage(recipient, text, priority)
        }
    }

    fun updateAiApiKey(key: String) {
        aiPreferences.setApiKey(key)
    }

    fun updateAiModel(model: String) {
        aiPreferences.setModel(model)
    }

    fun updateAiBaseUrl(url: String) {
        aiPreferences.setBaseUrl(url)
    }

    fun setTargetLanguage(language: String) {
        aiPreferences.setTargetLanguage(language)
        _messageTranslations.value = emptyMap()
    }

    fun summarizeThread(peerId: String, peerName: String, messages: List<LynqMessage>, targetLang: String? = null) {
        viewModelScope.launch {
            _isSummarizing.value = true
            try {
                val result = aiClient.summarizeConversation(
                    peerName = peerName,
                    messages = messages,
                    targetLanguage = targetLang ?: aiTargetLanguage.value,
                )
                result.onSuccess { summary ->
                    val updated = _threadSummaries.value.toMutableMap()
                    updated[peerId] = summary
                    _threadSummaries.value = updated
                }
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun summarizeMeshSitRep(messages: List<LynqMessage>, targetLang: String? = null) {
        viewModelScope.launch {
            _isSummarizing.value = true
            try {
                val result = aiClient.generateMeshSitRep(
                    allMessages = messages,
                    targetLanguage = targetLang ?: aiTargetLanguage.value,
                )
                result.onSuccess { summary ->
                    val updated = _threadSummaries.value.toMutableMap()
                    updated["__MESH_ALL__"] = summary
                    _threadSummaries.value = updated
                }
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun translateMessage(messageId: String, text: String, targetLang: String? = null) {
        viewModelScope.launch {
            val translating = _isTranslatingMap.value.toMutableMap()
            translating[messageId] = true
            _isTranslatingMap.value = translating
            try {
                val lang = targetLang ?: aiTargetLanguage.value
                val result = aiClient.translateText(text, lang)
                result.onSuccess { translated ->
                    val map = _messageTranslations.value.toMutableMap()
                    map[messageId] = translated
                    _messageTranslations.value = map
                }
            } finally {
                val done = _isTranslatingMap.value.toMutableMap()
                done[messageId] = false
                _isTranslatingMap.value = done
            }
        }
    }

    fun toggleMessageTranslation(messageId: String, text: String, targetLang: String? = null) {
        if (_messageTranslations.value.containsKey(messageId)) {
            val map = _messageTranslations.value.toMutableMap()
            map.remove(messageId)
            _messageTranslations.value = map
        } else {
            translateMessage(messageId, text, targetLang)
        }
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
