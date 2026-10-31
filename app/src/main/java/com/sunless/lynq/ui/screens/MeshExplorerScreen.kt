package com.sunless.lynq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sunless.lynq.demo.DemoData
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.ui.LynqViewModel
import com.sunless.lynq.ui.MeshViewMode
import com.sunless.lynq.ui.components.AppleDivider
import com.sunless.lynq.ui.components.AppleGroupedCard
import com.sunless.lynq.ui.components.CmdBar
import com.sunless.lynq.ui.components.MeshGraph
import com.sunless.lynq.ui.components.ModernRouteTrace
import com.sunless.lynq.ui.components.SectionHeader
import com.sunless.lynq.ui.components.StatusChip
import com.sunless.lynq.ui.components.TopBar
import com.sunless.lynq.ui.components.UserAvatar
import com.sunless.lynq.ui.navigation.Screen
import com.sunless.lynq.ui.navigation.navigateTab
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.Lynq
import com.sunless.lynq.util.asRelative
import kotlinx.coroutines.launch

/**
 * Obsidian-Inspired Mesh Explorer Screen.
 *
 * Protagonist: Expansive Obsidian Knowledge Graph Canvas.
 * - Deep dark charcoal atmosphere
 * - Touch-interactive constellation with live harmonic drift
 * - Live Mesh (Real hardware devices via Nearby Connections)
 * - Demo Mode (Simulated 4-node relay with Aarav, Kabir, Meera, Priya)
 * - Tap node to inspect relationships and highlight connected sub-graph
 * - Clean Apple bottom drawer for directory and transmission history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshExplorerScreen(
    navController: NavHostController,
    viewModel: LynqViewModel = viewModel(),
) {
    val pal = Lynq.pal
    val meshMode by viewModel.meshMode.collectAsStateWithLifecycle()
    val activePeers by viewModel.activePeers.collectAsStateWithLifecycle()
    val activeReachable by viewModel.activeReachableCount.collectAsStateWithLifecycle()
    val activeRelays by viewModel.activeRelayCount.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val lastSentSos by viewModel.lastSentSos.collectAsStateWithLifecycle()

    val isDemo = meshMode == MeshViewMode.DEMO
    val liveRoute = if (isDemo) {
        lastSentSos?.routeHistory ?: listOf("You", "Aarav", "Kabir", "Priya")
    } else {
        lastSentSos?.routeHistory ?: (if (activePeers.any { it.connected }) listOf("You", activePeers.first { it.connected }.name) else listOf("You"))
    }

    var selectedPeerName by remember { mutableStateOf<String?>(null) }
    var showDirectorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(reachableCount = activeReachable, title = "Mesh Graph")

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            // Mode Selector Pill: Live Mesh vs Demo Simulation
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = pal.surface.copy(alpha = 0.94f),
                border = BorderStroke(0.5.dp, pal.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val isLiveMode = meshMode == MeshViewMode.LIVE
                    // Live Mesh Tab
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (isLiveMode) (if (pal.isDark) Color(0xFF1E222D) else Color.White) else Color.Transparent,
                        border = if (isLiveMode) BorderStroke(0.5.dp, pal.border) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setMeshMode(MeshViewMode.LIVE) },
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (activeReachable > 0) pal.accentGreen else pal.accentBlue, CircleShape),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (activeReachable > 0) "Live Mesh ($activeReachable)" else "Live Mesh",
                                style = AppleCaption,
                                fontWeight = if (isLiveMode) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isLiveMode) pal.textPrimary else pal.textSecondary,
                            )
                        }
                    }

                    // Demo Simulation Tab
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (!isLiveMode) (if (pal.isDark) Color(0xFF1E222D) else Color.White) else Color.Transparent,
                        border = if (!isLiveMode) BorderStroke(0.5.dp, pal.border) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setMeshMode(MeshViewMode.DEMO) },
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFAF52DE), CircleShape),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Demo Simulation",
                                style = AppleCaption,
                                fontWeight = if (!isLiveMode) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isLiveMode) pal.textPrimary else pal.textSecondary,
                            )
                        }
                    }
                }
            }

            // Protagonist: Expansive Obsidian Knowledge Graph Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                MeshGraph(
                    peers = activePeers,
                    liveRoute = liveRoute,
                    isDemoMode = isDemo,
                    interactive = true,
                    selectedNodeName = selectedPeerName,
                    onNodeSelected = { selectedPeerName = it },
                    onOpenMessage = { peerIdOrName ->
                        val p = activePeers.find { it.id == peerIdOrName || it.name.equals(peerIdOrName, true) }
                        viewModel.openThread(p?.id ?: peerIdOrName)
                        navController.navigateTab(Screen.Messages.route)
                    },
                )
            }

            Spacer(Modifier.height(10.dp))

            // Cluster Status Capsule
            val capsuleText = if (isDemo) {
                "4 Simulated Nodes · 2 Relays · Aarav ↔ Kabir Bridge"
            } else if (activePeers.isNotEmpty()) {
                "${activePeers.size} Discovered Device${if (activePeers.size > 1) "s" else ""} · $activeReachable Online · Hop Limit 6"
            } else {
                "Scanning via Bluetooth & Wi-Fi Direct · 0 Connected"
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = pal.surface.copy(alpha = 0.94f),
                border = BorderStroke(0.5.dp, pal.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDirectorySheet = true },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isDemo) Color(0xFFAF52DE) else if (activeReachable > 0) pal.accentGreen else pal.accentBlue, CircleShape),
                        )
                        Text(
                            text = capsuleText,
                            style = AppleCaption,
                            fontSize = 11.sp,
                            color = pal.textSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                            contentDescription = "Directory",
                            tint = pal.accentBlue,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Nodes",
                            style = AppleCaption,
                            color = pal.accentBlue,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // Modal Peer Directory & Transmission History Sheet
    if (showDirectorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showDirectorySheet = false },
            sheetState = sheetState,
            containerColor = pal.surface,
            contentColor = pal.textPrimary,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(pal.textMuted.copy(alpha = 0.4f), RoundedCornerShape(2.dp)),
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 36.dp),
            ) {
                SectionHeader(
                    title = if (isDemo) "Demo Constellation Nodes" else "Discovered Mesh Devices",
                    actionText = if (isDemo) "4 Nodes" else "${activePeers.size} Nearby",
                )
                Spacer(Modifier.height(10.dp))

                if (!isDemo && activePeers.isEmpty()) {
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
                                text = "Scanning for Nearby Lynq Phones",
                                style = AppleHeadline,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.textPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Bring another phone with Lynq open nearby. Make sure Bluetooth and Location are ON. Devices will connect automatically.\n\nTap 'Demo Simulation' to view the multi-hop demo.",
                                style = AppleFootnote,
                                color = pal.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    AppleGroupedCard {
                        activePeers.forEachIndexed { index, peer ->
                            if (index > 0) AppleDivider()
                            PeerRow(peer = peer, onClick = {
                                selectedPeerName = peer.name
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showDirectorySheet = false
                                }
                            })
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                SectionHeader(title = "Recent Transmissions")
                Spacer(Modifier.height(10.dp))

                val activityList = if (isDemo) DemoData.activity else messages
                AppleGroupedCard {
                    if (activityList.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "No messages sent yet",
                                style = AppleFootnote,
                                color = pal.textMuted,
                            )
                        }
                    } else {
                        activityList.take(3).forEachIndexed { index, msg ->
                            if (index > 0) AppleDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = msg.senderName,
                                        style = AppleHeadline,
                                        color = pal.textPrimary,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    ModernRouteTrace(route = msg.routeHistory)
                                }
                                StatusChip(status = msg.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerRow(peer: Peer, onClick: () -> Unit) {
    val pal = Lynq.pal
    val isDirect = peer.hops <= 1 || peer.connected

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        UserAvatar(
            name = peer.name,
            size = 44.dp,
            isRelay = peer.relaying,
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = peer.name,
                    style = AppleHeadline,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.textPrimary,
                )
                Text(
                    text = if (peer.connected) "Connected" else if (isDirect) "Direct link" else "${peer.hops} hops",
                    style = AppleCaption,
                    color = if (peer.connected) pal.accentGreen else if (isDirect) pal.accentBlue else pal.textMuted,
                )
            }

            Spacer(Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${peer.id.take(8)} · ${if (isDirect) "Direct P2P Link" else "Relayed Node"}",
                    style = AppleFootnote,
                    color = pal.textSecondary,
                )
                Text(
                    text = peer.lastSeen.asRelative(),
                    style = AppleCaption,
                    color = pal.textMuted,
                )
            }
        }
    }
}