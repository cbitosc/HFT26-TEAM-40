package com.sunless.lynq.network

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.sunless.lynq.domain.model.Peer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles raw peer discovery, connection establishment, and byte payload transmission
 * using Google Nearby Connections with Strategy.P2P_CLUSTER.
 */
class NearbyManager(
    private val context: Context,
    val localDeviceId: String,
    initialLocalDeviceName: String,
) {
    var localDeviceName: String = initialLocalDeviceName
        private set

    fun updateLocalDeviceName(newName: String) {
        val clean = newName.trim()
        if (clean.isNotBlank() && clean != localDeviceName) {
            localDeviceName = clean
            android.util.Log.i(tag, "Local device name updated to: $localDeviceName")
            if (isAdvertising) {
                scope.launch {
                    try { client.stopAdvertising() } catch (_: Exception) {}
                    isAdvertising = false
                    startAdvertising()
                }
            }
        }
    }

    private val tag = "LynqNearbyManager"
    private val serviceId = "com.sunless.lynq.mesh"
    private val strategy = Strategy.P2P_CLUSTER

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client: ConnectionsClient by lazy { Nearby.getConnectionsClient(context) }

    // Map: endpointId -> ConnectedEndpoint
    data class ConnectedEndpoint(
        val endpointId: String,
        val peerId: String,
        val peerName: String,
        val connectedAt: Long = System.currentTimeMillis(),
    )

    private val connectedEndpoints = ConcurrentHashMap<String, ConnectedEndpoint>()
    private val connectingEndpoints = ConcurrentHashMap.newKeySet<String>()
    private val pendingEndpointInfo = ConcurrentHashMap<String, Pair<String, String>>()

    private val _connectedPeersFlow = MutableStateFlow<List<Peer>>(emptyList())
    val connectedPeersFlow: StateFlow<List<Peer>> = _connectedPeersFlow.asStateFlow()

    val connectedEndpointCount: Int get() = connectedEndpoints.size
    fun isEndpointConnected(endpointId: String?): Boolean = endpointId != null && connectedEndpoints.containsKey(endpointId)

    // Incoming raw payloads: Pair(endpointId, rawBytes)
    private val _rawPayloadFlow = MutableSharedFlow<Pair<String, ByteArray>>(extraBufferCapacity = 64)
    val rawPayloadFlow: SharedFlow<Pair<String, ByteArray>> = _rawPayloadFlow.asSharedFlow()

    private var isAdvertising = false
    private var isDiscovering = false

    /** Compact local endpoint advertisement: "DisplayName#DeviceId" */
    private val endpointName: String get() = "$localDeviceName#$localDeviceId"

    fun start(forceRestart: Boolean = false) {
        if (!forceRestart && isAdvertising && isDiscovering) {
            Log.d(tag, "Nearby advertising & discovery already running, skipping restart")
            return
        }
        Log.i(tag, "Starting Nearby Connections advertising and discovery (P2P_CLUSTER) for $endpointName")
        scope.launch {
            try {
                client.stopAdvertising()
                client.stopDiscovery()
            } catch (_: Exception) {}
            isAdvertising = false
            isDiscovering = false
            startAdvertising()
            startDiscovery()
        }
    }

    fun stop() {
        Log.i(tag, "Stopping Nearby Connections")
        try {
            client.stopAdvertising()
            client.stopDiscovery()
            client.stopAllEndpoints()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping nearby client", e)
        }
        isAdvertising = false
        isDiscovering = false
        connectedEndpoints.clear()
        connectingEndpoints.clear()
        pendingEndpointInfo.clear()
        _connectedPeersFlow.value = emptyList()
    }

    private fun startAdvertising() {
        if (isAdvertising) return
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(endpointName, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener {
                Log.i(tag, "Advertising active as: $endpointName")
                isAdvertising = true
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Advertising failed: ${e.message}, will retry in 3s")
                isAdvertising = false
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (!isAdvertising) startAdvertising()
                }
            }
    }

    private fun startDiscovery() {
        if (isDiscovering) return
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener {
                Log.i(tag, "Discovery active for service $serviceId")
                isDiscovering = true
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Discovery failed: ${e.message}, will retry in 3s")
                isDiscovering = false
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (!isDiscovering) startDiscovery()
                }
            }
    }

    private fun parseEndpointName(raw: String): Pair<String, String>? {
        val parts = raw.split("#")
        val name = parts.getOrNull(0)?.trim()
        val id = parts.getOrNull(1)?.trim()
        if (!name.isNullOrBlank() && !id.isNullOrBlank()) {
            return name to id
        }
        return null
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(tag, "Discovered endpoint: $endpointId (${info.endpointName})")
            if (connectingEndpoints.contains(endpointId) || connectedEndpoints.containsKey(endpointId)) {
                return
            }

            val parsed = parseEndpointName(info.endpointName)
            val pName = parsed?.first ?: ""
            val pId = parsed?.second ?: endpointId
            if (parsed != null) {
                pendingEndpointInfo[endpointId] = parsed
            }

            // Deterministic initiator tie-breaking to avoid P2P_CLUSTER simultaneous request collisions:
            // Device with lexicographically smaller deviceId acts as initiator.
            val isInitiator = localDeviceId < pId

            if (isInitiator) {
                connectingEndpoints.add(endpointId)
                Log.i(tag, "Acting as initiator for $endpointId ($pName). Requesting connection...")
                client.requestConnection(endpointName, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.i(tag, "Connection request submitted to $endpointId")
                    }
                    .addOnFailureListener { e ->
                        Log.w(tag, "Connection request failed to $endpointId: ${e.message}")
                        connectingEndpoints.remove(endpointId)
                    }
            } else {
                Log.i(tag, "Acting as responder for $endpointId ($pName), waiting for incoming request...")
                // Fallback: if initiator's discovery was delayed and no connection starts within 1500ms, connect
                scope.launch {
                    kotlinx.coroutines.delay(1500)
                    if (!connectedEndpoints.containsKey(endpointId) && !connectingEndpoints.contains(endpointId)) {
                        connectingEndpoints.add(endpointId)
                        Log.i(tag, "Fallback: responder initiating connection to $endpointId...")
                        client.requestConnection(endpointName, endpointId, connectionLifecycleCallback)
                            .addOnFailureListener {
                                connectingEndpoints.remove(endpointId)
                            }
                    }
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(tag, "Endpoint lost: $endpointId")
            connectingEndpoints.remove(endpointId)
            pendingEndpointInfo.remove(endpointId)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.i(tag, "Connection initiated from $endpointId (${info.endpointName}), accepting...")
            connectingEndpoints.add(endpointId)
            val parsed = parseEndpointName(info.endpointName)
            if (parsed != null) {
                pendingEndpointInfo[endpointId] = parsed
            }

            client.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    Log.i(tag, "acceptConnection called successfully for $endpointId")
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Accept connection failed for $endpointId: ${e.message}")
                    connectingEndpoints.remove(endpointId)
                    pendingEndpointInfo.remove(endpointId)
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            connectingEndpoints.remove(endpointId)
            val info = pendingEndpointInfo.remove(endpointId)
            val peerName = info?.first
            val peerId = info?.second

            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK,
                ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT -> {
                    Log.i(tag, "Connected to endpoint $endpointId ($peerName / $peerId)")
                    registerEndpoint(endpointId, peerId, peerName)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(tag, "Connection rejected by endpoint $endpointId")
                }
                else -> {
                    Log.w(tag, "Connection failed to endpoint $endpointId: ${resolution.status.statusMessage} (code ${resolution.status.statusCode})")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(tag, "Disconnected from endpoint $endpointId")
            unregisterEndpoint(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes()
                if (bytes != null) {
                    Log.i(tag, "Received ${bytes.size} raw bytes from endpoint $endpointId")
                    scope.launch {
                        _rawPayloadFlow.emit(endpointId to bytes)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Can be used for transfer progress if needed
        }
    }

    private fun registerEndpoint(endpointId: String, peerId: String?, peerName: String?) {
        val cleanName = peerName?.trim().orEmpty()
        val cleanId = peerId?.trim().orEmpty()
        val isValid = cleanName.isNotBlank() && !cleanName.startsWith("Peer-", ignoreCase = true) && cleanName != "Peer" && cleanId.length > 4

        val endpoint = ConnectedEndpoint(
            endpointId = endpointId,
            peerId = if (isValid) cleanId else endpointId,
            peerName = if (isValid) cleanName else "",
            connectedAt = System.currentTimeMillis(),
        )
        connectedEndpoints[endpointId] = endpoint
        updatePeersList()
    }

    fun updateEndpointIdentity(endpointId: String, peerId: String, peerName: String) {
        val cleanName = peerName.trim()
        val cleanId = peerId.trim()
        if (cleanName.isBlank() || cleanName.startsWith("Peer-", ignoreCase = true) || cleanName == "Peer" || cleanId.length <= 4) {
            return
        }
        val existing = connectedEndpoints[endpointId]
        if (existing != null && existing.peerId == cleanId && existing.peerName == cleanName) {
            return
        }
        val connectedAt = existing?.connectedAt ?: System.currentTimeMillis()
        connectedEndpoints[endpointId] = ConnectedEndpoint(
            endpointId = endpointId,
            peerId = cleanId,
            peerName = cleanName,
            connectedAt = connectedAt,
        )
        updatePeersList()
    }

    private fun unregisterEndpoint(endpointId: String) {
        connectedEndpoints.remove(endpointId)
        connectingEndpoints.remove(endpointId)
        updatePeersList()
    }

    private fun updatePeersList() {
        val list = connectedEndpoints.values
            .filter { it.peerName.isNotBlank() && !it.peerName.startsWith("Peer-", ignoreCase = true) && it.peerName != "Peer" && it.peerId.length > 4 }
            .map {
                Peer(
                    id = it.peerId,
                    name = it.peerName,
                    connected = true,
                    hops = 1,
                    lastSeen = it.connectedAt,
                )
            }.sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
        _connectedPeersFlow.value = list
    }

    fun getEndpointIdForPeer(peerId: String): String? {
        return connectedEndpoints.entries.firstOrNull { it.value.peerId == peerId }?.key
    }

    /**
     * Send byte array directly to a specific connected endpoint.
     */
    fun sendBytes(endpointId: String, bytes: ByteArray): Boolean {
        return try {
            client.sendPayload(endpointId, Payload.fromBytes(bytes))
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to send bytes to $endpointId", e)
            false
        }
    }

    /**
     * Flood bytes to all currently connected endpoints except [excludeEndpointId].
     * Returns the count of endpoints successfully forwarded to.
     */
    fun flood(bytes: ByteArray, excludeEndpointId: String? = null): Int {
        var count = 0
        val targets = connectedEndpoints.keys.filter { it != excludeEndpointId }
        for (target in targets) {
            if (sendBytes(target, bytes)) {
                count++
            }
        }
        return count
    }

    /** Current direct connected peers count */
    val directPeerCount: Int get() = connectedEndpoints.size
}
