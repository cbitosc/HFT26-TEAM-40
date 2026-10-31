package com.sunless.lynq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunless.lynq.demo.DemoData
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.Priority
import com.sunless.lynq.ui.components.AppleDivider
import com.sunless.lynq.ui.components.AppleGroupedCard
import com.sunless.lynq.ui.components.CmdBar
import com.sunless.lynq.ui.components.MeshGraph
import com.sunless.lynq.ui.components.MetricCard
import com.sunless.lynq.ui.components.ModernRouteTrace
import com.sunless.lynq.ui.components.PriorityBadge
import com.sunless.lynq.ui.components.SectionHeader
import com.sunless.lynq.ui.components.SosLever
import com.sunless.lynq.ui.components.StatusChip
import com.sunless.lynq.ui.components.TopBar
import com.sunless.lynq.ui.components.UserAvatar
import com.sunless.lynq.ui.navigation.Screen
import com.sunless.lynq.ui.navigation.navigateTab
import com.sunless.lynq.ui.theme.AppleBody
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.Lynq
import com.sunless.lynq.util.asRelative

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunless.lynq.ui.LynqViewModel

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: LynqViewModel = viewModel(),
) {
    val pal = Lynq.pal

    val meshMode by viewModel.meshMode.collectAsStateWithLifecycle()
    val activePeers by viewModel.activePeers.collectAsStateWithLifecycle()
    val activeReachable by viewModel.activeReachableCount.collectAsStateWithLifecycle()
    val activeRelays by viewModel.activeRelayCount.collectAsStateWithLifecycle()
    val pendingMessages by viewModel.pendingMessages.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val lastSentSos by viewModel.lastSentSos.collectAsStateWithLifecycle()

    val isDemo = meshMode == com.sunless.lynq.ui.MeshViewMode.DEMO
    val liveRoute = if (isDemo) {
        lastSentSos?.routeHistory ?: listOf("You", "Aarav", "Kabir", "Priya")
    } else {
        lastSentSos?.routeHistory ?: (if (activePeers.any { it.connected }) listOf("You", activePeers.first { it.connected }.name) else listOf("You"))
    }

    val effectivePending = pendingMessages.size
    val effectiveActivity = if (isDemo) DemoData.activity else messages

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
            TopBar(reachableCount = activeReachable, title = "Network")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Headline Status
                val subheadText = if (isDemo) {
                    "Demo Simulation Mode · 4-node virtual cluster"
                } else if (activeReachable > 0) {
                    "Decentralized mesh active · $activeReachable device${if (activeReachable > 1) "s" else ""} connected"
                } else {
                    "Decentralized mesh active · Scanning via Bluetooth & Wi-Fi"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = subheadText,
                        style = AppleSubhead,
                        color = pal.textSecondary,
                        modifier = Modifier.weight(1f),
                    )

                    // Quick Mode Switch Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDemo) Color(0xFFAF52DE).copy(alpha = 0.16f) else pal.accentGreen.copy(alpha = 0.16f),
                        border = BorderStroke(0.5.dp, if (isDemo) Color(0xFFAF52DE).copy(alpha = 0.4f) else pal.accentGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            viewModel.setMeshMode(if (isDemo) com.sunless.lynq.ui.MeshViewMode.LIVE else com.sunless.lynq.ui.MeshViewMode.DEMO)
                        },
                    ) {
                        Text(
                            text = if (isDemo) "DEMO" else "LIVE",
                            style = AppleCaption,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDemo) Color(0xFFAF52DE) else pal.accentGreen,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }

                // Protagonist: Live Obsidian Mesh Graph
                MeshGraph(
                    peers = activePeers,
                    liveRoute = liveRoute,
                    isDemoMode = isDemo,
                    height = 230.dp,
                    interactive = true,
                    onOpenMessage = { peerIdOrName ->
                        val p = activePeers.find { it.id == peerIdOrName || it.name.equals(peerIdOrName, true) }
                        viewModel.openThread(p?.id ?: peerIdOrName)
                        navController.navigateTab(Screen.Messages.route)
                    },
                )

                Spacer(Modifier.height(16.dp))

                // Apple Emergency SOS Trigger
                SosLever(
                    onClick = { navController.navigateTab(Screen.Emergency.route) },
                )

                Spacer(Modifier.height(16.dp))

                // Clean Apple Metric Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCard(
                        value = "$activeReachable",
                        label = "Reachable",
                        accentColor = pal.accentBlue,
                        icon = Icons.Rounded.Sensors,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        value = "$activeRelays",
                        label = "Relaying",
                        accentColor = pal.accentGreen,
                        icon = Icons.Rounded.Hub,
                        modifier = Modifier.weight(1f),
                    )
                    MetricCard(
                        value = "$effectivePending",
                        label = "Queued",
                        accentColor = pal.warningAmber,
                        icon = Icons.Rounded.Schedule,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Apple Inset Grouped Section for Activity
                SectionHeader(
                    title = "Recent Transmissions",
                    actionText = "See All",
                    onActionClick = { navController.navigateTab(Screen.Messages.route) },
                )

                Spacer(Modifier.height(10.dp))

                AppleGroupedCard {
                    effectiveActivity.take(4).forEachIndexed { index, msg ->
                        if (index > 0) AppleDivider()
                        ActivityRow(
                            who = msg.senderName,
                            body = msg.body,
                            route = msg.routeHistory,
                            status = msg.status,
                            priority = msg.priority,
                            at = msg.createdAt,
                            onClick = { navController.navigateTab(Screen.Messages.route) },
                        )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    who: String,
    body: String,
    route: List<String>,
    status: DeliveryStatus,
    priority: Priority,
    at: Long,
    onClick: () -> Unit,
) {
    val pal = Lynq.pal
    val isCritical = priority == Priority.CRITICAL

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UserAvatar(
            name = who,
            size = 40.dp,
            isCritical = isCritical,
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = who,
                    style = AppleHeadline,
                    color = pal.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = at.asRelative(),
                    style = AppleCaption,
                    color = pal.textMuted,
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = body,
                style = AppleFootnote,
                color = if (isCritical) pal.emergencyRed else pal.textSecondary,
                maxLines = 1,
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModernRouteTrace(route = route)
                StatusChip(status = status)
            }
        }
    }
}