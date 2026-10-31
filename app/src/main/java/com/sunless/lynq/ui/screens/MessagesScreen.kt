package com.sunless.lynq.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sunless.lynq.ai.LanguageRegistry
import com.sunless.lynq.ai.ThreadSummary
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.LynqMessage
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.domain.model.Priority
import com.sunless.lynq.ui.LynqViewModel
import com.sunless.lynq.ui.components.AppleDivider
import com.sunless.lynq.ui.components.AppleGroupedCard
import com.sunless.lynq.ui.components.ModernRouteTrace
import com.sunless.lynq.ui.components.StatusChip
import com.sunless.lynq.ui.components.TopBar
import com.sunless.lynq.ui.components.UserAvatar
import com.sunless.lynq.ui.theme.AppleBody
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.Lynq
import com.sunless.lynq.util.asClock
import com.sunless.lynq.util.asRelative

private data class ChatBubble(
    val id: String,
    val isMine: Boolean,
    val body: String,
    val time: Long,
    val status: DeliveryStatus,
    val route: List<String>,
)

@Composable
fun MessagesScreen(
    navController: NavHostController,
    viewModel: LynqViewModel = viewModel(),
) {
    val openPeerId by viewModel.openPeerId.collectAsStateWithLifecycle()
    val activePeers by viewModel.activePeers.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val activeReachable by viewModel.activeReachableCount.collectAsStateWithLifecycle()

    androidx.activity.compose.BackHandler(enabled = openPeerId != null) {
        viewModel.openThread(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        if (openPeerId == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
            ) {
                ThreadList(
                    reachableCount = activeReachable,
                    peers = activePeers,
                    messages = messages,
                    viewModel = viewModel,
                    onOpen = { viewModel.openThread(it) },
                )
            }
        } else {
            val selectedPeer = activePeers.firstOrNull { it.id == openPeerId } ?: activePeers.firstOrNull()
            if (selectedPeer != null) {
                OpenThreadView(
                    peer = selectedPeer,
                    messages = messages
                        .filter { it.senderId == selectedPeer.id || it.recipientId == selectedPeer.id }
                        .sortedBy { it.createdAt },
                    viewModel = viewModel,
                    onSend = { text -> viewModel.sendChatMessage(selectedPeer, text) },
                    onBack = { viewModel.openThread(null) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ThreadList(
    reachableCount: Int,
    peers: List<Peer>,
    messages: List<LynqMessage>,
    viewModel: LynqViewModel,
    onOpen: (String) -> Unit,
) {
    val pal = Lynq.pal
    val threadSummaries by viewModel.threadSummaries.collectAsStateWithLifecycle()
    val isSummarizing by viewModel.isSummarizing.collectAsStateWithLifecycle()
    val apiKey by viewModel.aiApiKey.collectAsStateWithLifecycle()
    val targetLang by viewModel.aiTargetLanguage.collectAsStateWithLifecycle()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }

    val meshSitRep = threadSummaries["__MESH_ALL__"]

    if (showApiKeyDialog) {
        FeatherlessApiKeyDialog(
            currentKey = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = {
                viewModel.updateAiApiKey(it)
                showApiKeyDialog = false
            },
        )
    }

    if (showLangDialog) {
        LanguageSelectorDialog(
            currentLang = targetLang,
            onDismiss = { showLangDialog = false },
            onSelect = {
                viewModel.setTargetLanguage(it)
                showLangDialog = false
            },
        )
    }

    TopBar(reachableCount = reachableCount, title = "Messages")

    val sortedPeers = remember(peers, messages) {
        peers.sortedWith(
            compareByDescending<Peer> { peer ->
                messages.filter { it.senderId == peer.id || it.recipientId == peer.id }
                    .maxOfOrNull { it.createdAt } ?: 0L
            }.thenByDescending { it.connected }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Direct & multi-hop encrypted channels",
            style = AppleSubhead,
            color = pal.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // AI Mesh Situational Report (SitRep) Banner
        AppleGroupedCard(modifier = Modifier.padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(pal.accentBlue.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = pal.accentBlue,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "AI Mesh SitRep",
                                style = AppleHeadline,
                                fontWeight = FontWeight.Bold,
                                color = pal.textPrimary,
                            )
                            Text(
                                text = "🟢 Lynq AI Cloud Ready",
                                style = AppleCaption,
                                color = pal.accentGreen,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Quick language badge
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = pal.surfaceElevated,
                            modifier = Modifier.clickable { showLangDialog = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = LanguageRegistry.find(targetLang).flag,
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = targetLang,
                                    style = AppleCaption,
                                    color = pal.textPrimary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (meshSitRep != null) {
                    // Display existing mesh SitRep
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TriageLevelBadge(level = meshSitRep.triageLevel)
                        Text(
                            text = meshSitRep.category,
                            style = AppleCaption,
                            color = pal.accentBlue,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = meshSitRep.overview,
                        style = AppleBody.copy(fontSize = 14.sp),
                        color = pal.textPrimary,
                    )

                    Spacer(Modifier.height(8.dp))
                    meshSitRep.keyPoints.take(2).forEach { pt ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 1.dp),
                        ) {
                            Text(text = "•", color = pal.accentBlue, style = AppleCaption)
                            Text(text = pt, style = AppleFootnote, color = pal.textSecondary)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Model: ${meshSitRep.modelUsed.substringAfterLast("/")}",
                            style = AppleCaption,
                            color = pal.textMuted,
                        )

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = pal.accentBlue.copy(alpha = 0.15f),
                            modifier = Modifier.clickable(enabled = !isSummarizing) {
                                viewModel.summarizeMeshSitRep(messages)
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isSummarizing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = pal.accentBlue,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = pal.accentBlue,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                                Text(
                                    text = if (isSummarizing) "Analyzing..." else "Refresh SitRep",
                                    style = AppleCaption,
                                    color = pal.accentBlue,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                } else {
                    // Prompt to generate mesh SitRep
                    Text(
                        text = "Synthesize all peer conversations, emergency requests, and relay hops into a unified disaster situational report using Featherless AI.",
                        style = AppleFootnote,
                        color = pal.textSecondary,
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pal.accentBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSummarizing) {
                                viewModel.summarizeMeshSitRep(messages)
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isSummarizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing Mesh Traffic...",
                                    style = AppleFootnote,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Generate AI Mesh SitRep ($targetLang)",
                                    style = AppleFootnote,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (peers.isEmpty()) {
            AppleGroupedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sensors,
                        contentDescription = null,
                        tint = pal.accentBlue,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No Connected Devices",
                        style = AppleHeadline,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.textPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "When another phone running Lynq connects over Bluetooth or Wi-Fi Direct, an end-to-end encrypted channel will appear here automatically.\n\nSwitch to Demo Mode from Mesh Graph to test simulated messaging.",
                        style = AppleFootnote,
                        color = pal.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            AppleGroupedCard {
                sortedPeers.forEachIndexed { index, peer ->
                    val peerMessages = messages.filter { it.senderId == peer.id || it.recipientId == peer.id }
                    val latest = peerMessages.maxByOrNull { it.createdAt }
                    val preview = latest?.body ?: "Encrypted channel ready (${if (peer.hops == 1) "direct" else "${peer.hops} hops"})"
                    val lastAt = latest?.createdAt ?: peer.lastSeen
                    val status = latest?.status ?: DeliveryStatus.DELIVERED

                    if (index > 0) AppleDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(peer.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatar(
                            name = peer.name,
                            isRelay = peer.relaying,
                            size = 46.dp,
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = peer.name,
                                        style = AppleHeadline,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.textPrimary,
                                    )
                                }

                                Text(
                                    text = lastAt.asRelative(),
                                    style = AppleCaption,
                                    color = pal.textMuted,
                                )
                            }

                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = preview,
                                style = AppleSubhead,
                                color = pal.textSecondary,
                                maxLines = 1,
                            )

                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusChip(status = status)
                                Text(
                                    text = if (peer.hops == 1) "Direct Link" else "${peer.hops} Hops Relay",
                                    style = AppleCaption,
                                    color = pal.textMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenThreadView(
    peer: Peer,
    messages: List<LynqMessage>,
    viewModel: LynqViewModel,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal
    val focusManager = LocalFocusManager.current
    var messageText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val threadSummaries by viewModel.threadSummaries.collectAsStateWithLifecycle()
    val isSummarizing by viewModel.isSummarizing.collectAsStateWithLifecycle()
    val apiKey by viewModel.aiApiKey.collectAsStateWithLifecycle()
    val targetLang by viewModel.aiTargetLanguage.collectAsStateWithLifecycle()
    val messageTranslations by viewModel.messageTranslations.collectAsStateWithLifecycle()
    val isTranslatingMap by viewModel.isTranslatingMap.collectAsStateWithLifecycle()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var isSummaryExpanded by remember { mutableStateOf(true) }

    val summary = threadSummaries[peer.id]

    val now = remember { System.currentTimeMillis() }

    // Demo fixture messages if user has not exchanged real packets with this peer yet
    val effectiveMessages = remember(messages, peer) {
        if (messages.isNotEmpty()) messages else {
            listOf(
                LynqMessage("demo-1", MessageType.CHAT, Priority.NORMAL, peer.id, peer.name, "me", "Are you all safe? Cellular and internet are completely down here.", now - 45 * 60_000),
                LynqMessage("demo-2", MessageType.CHAT, Priority.NORMAL, "me", "You", peer.id, "We're safe. We formed a mesh at the east shelter with battery packs.", now - 44 * 60_000),
                LynqMessage("demo-3", MessageType.CHAT, Priority.NORMAL, peer.id, peer.name, "me", "Got it, two more people just connected through Aarav's phone.", now - 8 * 60_000),
                LynqMessage("demo-4", MessageType.CHAT, Priority.NORMAL, "me", "You", peer.id, "Keep Lynq running in background. All packets will forward seamlessly.", now - 6 * 60_000),
            )
        }
    }

    if (showApiKeyDialog) {
        FeatherlessApiKeyDialog(
            currentKey = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = {
                viewModel.updateAiApiKey(it)
                showApiKeyDialog = false
            },
        )
    }

    if (showLangDialog) {
        LanguageSelectorDialog(
            currentLang = targetLang,
            onDismiss = { showLangDialog = false },
            onSelect = {
                viewModel.setTargetLanguage(it)
                showLangDialog = false
            },
        )
    }

    LaunchedEffect(messages.size, scrollState.maxValue) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Conversation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = pal.accentBlue,
                )
            }

            UserAvatar(name = peer.name, size = 36.dp, isRelay = peer.relaying)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    style = AppleHeadline,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.textPrimary,
                )
                Text(
                    text = if (peer.hops == 1) "Direct Link · End-to-End Encrypted" else "${peer.hops} Hops Relay · End-to-End Encrypted",
                    style = AppleCaption,
                    color = pal.accentBlue,
                )
            }

            // Quick Language Switcher
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = pal.surfaceElevated,
                modifier = Modifier.clickable { showLangDialog = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(text = LanguageRegistry.find(targetLang).flag, fontSize = 12.sp)
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = "Language",
                        tint = pal.accentBlue,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(pal.accentGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Lynq AI Cloud Ready",
                    tint = pal.accentGreen,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        AppleDivider()

        // AI Situational Summary Top Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = pal.surface,
                border = BorderStroke(0.5.dp, pal.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header Bar of Summary Widget
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = pal.accentBlue,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Featherless AI Intel",
                                style = AppleHeadline,
                                fontWeight = FontWeight.Bold,
                                color = pal.textPrimary,
                                fontSize = 14.sp,
                            )
                            if (summary != null) {
                                TriageLevelBadge(level = summary.triageLevel)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Summarize Button
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = pal.accentBlue,
                                modifier = Modifier.clickable(enabled = !isSummarizing) {
                                    viewModel.summarizeThread(peer.id, peer.name, effectiveMessages, targetLang)
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (isSummarizing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(11.dp),
                                            strokeWidth = 1.5.dp,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "Analyzing...",
                                            style = AppleCaption,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    } else {
                                        Text(
                                            text = if (summary == null) "✨ Summarize" else "Refresh",
                                            style = AppleCaption,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }

                            if (summary != null) {
                                IconButton(
                                    onClick = { isSummaryExpanded = !isSummaryExpanded },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isSummaryExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        contentDescription = "Expand/Collapse",
                                        tint = pal.textSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Expandable Summary Details
                    if (summary != null) {
                        AnimatedVisibility(
                            visible = isSummaryExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                AppleDivider(modifier = Modifier.padding(bottom = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Category: ${summary.category}",
                                        style = AppleFootnote,
                                        color = pal.accentBlue,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Target: $targetLang",
                                        style = AppleCaption,
                                        color = pal.textMuted,
                                    )
                                }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = summary.overview,
                                    style = AppleBody.copy(fontSize = 13.5.sp),
                                    color = pal.textPrimary,
                                )

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Key Intelligence:",
                                    style = AppleCaption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pal.textSecondary,
                                )
                                summary.keyPoints.forEach { pt ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 1.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(text = "•", color = pal.accentBlue, style = AppleCaption)
                                        Text(text = pt, style = AppleFootnote, color = pal.textSecondary)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = pal.surfaceElevated,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lightbulb,
                                            contentDescription = null,
                                            tint = pal.warningAmber,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = summary.actionableAdvice,
                                            style = AppleCaption,
                                            color = pal.textPrimary,
                                        )
                                    }
                                }

                                if (summary.isOfflineEstimate) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "ℹ️ Offline heuristic estimate. Connect to Internet/Render backend for live LLM intelligence.",
                                        style = AppleCaption,
                                        color = pal.textMuted,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Message Bubbles Container
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focusManager.clearFocus()
                }
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val bubbleList = remember(messages, peer, now) {
                if (messages.isNotEmpty()) {
                    messages.sortedBy { it.createdAt }.map { msg ->
                        ChatBubble(
                            id = msg.messageId,
                            isMine = msg.senderName == "You" || msg.senderId != peer.id,
                            body = msg.body,
                            time = msg.createdAt,
                            status = msg.status,
                            route = if (msg.routeHistory.isNotEmpty()) msg.routeHistory else listOf("You", peer.name),
                        )
                    }
                } else {
                    listOf(
                        ChatBubble("demo-1", false, "Are you all safe? Cellular and internet are completely down here.", now - 45 * 60_000, DeliveryStatus.DELIVERED, listOf(peer.name, "You")),
                        ChatBubble("demo-2", true, "We're safe. We formed a mesh at the east shelter with battery packs.", now - 44 * 60_000, DeliveryStatus.DELIVERED, listOf("You", peer.name)),
                        ChatBubble("demo-3", false, "Got it, two more people just connected through Aarav's phone.", now - 8 * 60_000, DeliveryStatus.DELIVERED, listOf(peer.name, "You")),
                        ChatBubble("demo-4", true, "Keep Lynq running in background. All packets will forward seamlessly.", now - 6 * 60_000, DeliveryStatus.DELIVERED, listOf("You", "Meera", peer.name)),
                    )
                }
            }

            bubbleList.forEach { chat ->
                val translatedText = messageTranslations[chat.id]
                val isTranslating = isTranslatingMap[chat.id] == true

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (chat.isMine) Alignment.End else Alignment.Start,
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (chat.isMine) 18.dp else 4.dp,
                            bottomEnd = if (chat.isMine) 4.dp else 18.dp,
                        ),
                        color = if (chat.isMine) pal.accentBlue else pal.surfaceElevated,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .widthIn(max = 285.dp),
                        ) {
                            Text(
                                text = chat.body,
                                style = AppleBody,
                                color = if (chat.isMine) Color.White else pal.textPrimary,
                            )

                            // Translated Box (if translation requested)
                            if (translatedText != null) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (chat.isMine) Color.White.copy(alpha = 0.15f) else pal.accentBlue.copy(alpha = 0.10f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Translate,
                                                    contentDescription = null,
                                                    tint = if (chat.isMine) Color.White else pal.accentBlue,
                                                    modifier = Modifier.size(12.dp),
                                                )
                                                Text(
                                                    text = "Translated ($targetLang)",
                                                    style = AppleCaption,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (chat.isMine) Color.White else pal.accentBlue,
                                                    fontSize = 11.sp,
                                                )
                                            }

                                            Text(
                                                text = "Hide",
                                                style = AppleCaption,
                                                color = if (chat.isMine) Color.White.copy(alpha = 0.7f) else pal.textMuted,
                                                fontSize = 11.sp,
                                                modifier = Modifier.clickable {
                                                    viewModel.toggleMessageTranslation(chat.id, chat.body, targetLang)
                                                },
                                            )
                                        }

                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = translatedText,
                                            style = AppleBody.copy(fontSize = 13.5.sp),
                                            color = if (chat.isMine) Color.White else pal.textPrimary,
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = chat.time.asClock(),
                                        style = AppleCaption,
                                        color = if (chat.isMine) Color.White.copy(alpha = 0.75f) else pal.textMuted,
                                    )
                                    if (chat.isMine) {
                                        val (tickIcon, tickColor) = when (chat.status) {
                                            DeliveryStatus.DELIVERED, DeliveryStatus.RELAYED ->
                                                Icons.Rounded.DoneAll to Color.White.copy(alpha = 0.95f)
                                            DeliveryStatus.SENT ->
                                                Icons.Rounded.Check to Color.White.copy(alpha = 0.75f)
                                            DeliveryStatus.PENDING ->
                                                Icons.Rounded.Schedule to Color.White.copy(alpha = 0.6f)
                                            DeliveryStatus.FAILED, DeliveryStatus.EXPIRED ->
                                                Icons.Rounded.ErrorOutline to pal.emergencyRed
                                        }
                                        Icon(
                                            imageVector = tickIcon,
                                            contentDescription = chat.status.name,
                                            tint = tickColor,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }

                                // Quick Translate Action Trigger
                                if (translatedText == null) {
                                    Row(
                                        modifier = Modifier.clickable {
                                            viewModel.translateMessage(chat.id, chat.body, targetLang)
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        if (isTranslating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.dp,
                                                color = if (chat.isMine) Color.White else pal.accentBlue,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Translate,
                                                contentDescription = "Translate",
                                                tint = if (chat.isMine) Color.White.copy(alpha = 0.7f) else pal.textMuted,
                                                modifier = Modifier.size(11.dp),
                                            )
                                        }
                                        Text(
                                            text = if (isTranslating) "Translating" else "Translate",
                                            style = AppleCaption,
                                            color = if (chat.isMine) Color.White.copy(alpha = 0.7f) else pal.textMuted,
                                            fontSize = 10.5.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Route breadcrumb (only show for incoming messages from others)
                    if (!chat.isMine) {
                        Spacer(Modifier.height(2.dp))
                        ModernRouteTrace(route = chat.route)
                    }
                }
            }
        }

        // Apple Style Message Input Capsule - Docked at the bottom
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = pal.surface,
            border = BorderStroke(0.5.dp, pal.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = AppleBody.copy(color = pal.textPrimary),
                    cursorBrush = SolidColor(pal.accentBlue),
                    decorationBox = { innerTextField ->
                        if (messageText.isEmpty()) {
                            Text(
                                text = "Encrypted iMessage / Mesh…",
                                style = AppleBody,
                                color = pal.textMuted,
                            )
                        }
                        innerTextField()
                    },
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(pal.accentBlue)
                        .clickable {
                            if (messageText.isNotBlank()) {
                                onSend(messageText.trim())
                                messageText = ""
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TriageLevelBadge(level: String) {
    val pal = Lynq.pal
    val (color, label) = when (level.uppercase()) {
        "CRITICAL" -> pal.emergencyRed to "CRITICAL"
        "HIGH" -> pal.warningAmber to "HIGH"
        "SAFE" -> pal.accentGreen to "SAFE"
        else -> pal.accentBlue to "NORMAL"
    }

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = 0.16f),
    ) {
        Text(
            text = label,
            style = AppleCaption,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun FeatherlessApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val pal = Lynq.pal
    var keyText by remember(currentKey) { mutableStateOf(currentKey) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = pal.accentBlue,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Featherless AI Configuration",
                    style = AppleHeadline,
                    fontWeight = FontWeight.Bold,
                    color = pal.textPrimary,
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter your Featherless.ai API Key to enable live Meta-Llama-3.1 / Mistral emergency summarization and crisis translation over the mesh.",
                    style = AppleFootnote,
                    color = pal.textSecondary,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    singleLine = true,
                    placeholder = { Text("e.g. sk-...") },
                    label = { Text("Featherless API Key") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle key visibility",
                                tint = pal.textSecondary,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (keyText.isNotBlank()) "Status: Key configured" else "Status: No key set (using local offline heuristic engine)",
                    style = AppleCaption,
                    color = if (keyText.isNotBlank()) pal.accentGreen else pal.warningAmber,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(keyText) },
            ) {
                Text("Save Key", color = pal.accentBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = pal.textSecondary)
            }
        },
    )
}

@Composable
fun LanguageSelectorDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val pal = Lynq.pal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    tint = pal.accentBlue,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Select Language",
                    style = AppleHeadline,
                    fontWeight = FontWeight.Bold,
                    color = pal.textPrimary,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                LanguageRegistry.languages.forEach { lang ->
                    val isSelected = lang.displayName.equals(currentLang, ignoreCase = true) ||
                            lang.code.equals(currentLang, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang.displayName) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(text = lang.flag, fontSize = 20.sp)
                            Column {
                                Text(
                                    text = lang.displayName,
                                    style = AppleHeadline,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) pal.accentBlue else pal.textPrimary,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    text = lang.nativeName,
                                    style = AppleCaption,
                                    color = pal.textSecondary,
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = pal.accentBlue,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    AppleDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = pal.accentBlue)
            }
        },
    )
}