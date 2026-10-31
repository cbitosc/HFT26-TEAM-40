package com.sunless.lynq.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunless.lynq.demo.DemoData
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.ui.components.AppleDivider
import com.sunless.lynq.ui.components.AppleGroupedCard
import com.sunless.lynq.ui.components.CmdBar
import com.sunless.lynq.ui.components.HoldToSosButton
import com.sunless.lynq.ui.components.HopMeter
import com.sunless.lynq.ui.components.HopStep
import com.sunless.lynq.ui.components.ModernRouteTrace
import com.sunless.lynq.ui.components.SectionHeader
import com.sunless.lynq.ui.components.StatusChip
import com.sunless.lynq.ui.components.TopBar
import com.sunless.lynq.ui.navigation.Screen
import com.sunless.lynq.ui.navigation.navigateTab
import com.sunless.lynq.ui.theme.AppleBody
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.AppleTitle2
import com.sunless.lynq.ui.theme.Lynq
import com.sunless.lynq.ui.theme.TechCodeStyle
import com.sunless.lynq.util.asRelative
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunless.lynq.ui.LynqViewModel

private data class CategoryItem(val name: String, val icon: ImageVector)

private val CATEGORIES = listOf(
    CategoryItem("Medical", Icons.Rounded.LocalHospital),
    CategoryItem("Fire", Icons.Rounded.LocalFireDepartment),
    CategoryItem("Trapped", Icons.Rounded.Emergency),
    CategoryItem("Threat", Icons.Rounded.Shield),
)

@Composable
fun EmergencyScreen(
    navController: NavHostController,
    viewModel: LynqViewModel = viewModel(),
) {
    val pal = Lynq.pal
    val activePeers by viewModel.activePeers.collectAsStateWithLifecycle()
    val reachableCount by viewModel.activeReachableCount.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val activeBroadcast by viewModel.activeSosBroadcasting.collectAsStateWithLifecycle()
    val lastSos by viewModel.lastSentSos.collectAsStateWithLifecycle()

    val effectiveReachable = reachableCount
    val effectiveAlerts = alerts

    var selectedCategory by rememberSaveable { mutableStateOf(CATEGORIES.first().name) }
    var note by rememberSaveable { mutableStateOf("") }
    var shareLocation by rememberSaveable { mutableStateOf(true) }

    val isBroadcasting = activeBroadcast

    androidx.activity.compose.BackHandler(enabled = isBroadcasting) {
        viewModel.cancelSos()
    }

    val transition = rememberInfiniteTransition(label = "sosPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        TopBar(reachableCount = effectiveReachable, title = "Emergency SOS")

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Offline peer-to-peer broadcast · Unconditionally prioritised",
                    style = AppleSubhead,
                    color = pal.textSecondary,
                    modifier = Modifier.padding(bottom = 18.dp),
                )

                if (!isBroadcasting) {
                    // Category Selection Pills
                    SectionHeader(title = "Emergency Type")
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CATEGORIES.forEach { cat ->
                            val selected = cat.name == selectedCategory
                            Surface(
                                onClick = { selectedCategory = cat.name },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) pal.emergencyRed else pal.surface,
                                border = if (selected) null else BorderStroke(0.5.dp, pal.border),
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.name,
                                        tint = if (selected) Color.White else pal.textSecondary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = AppleFootnote,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (selected) Color.White else pal.textPrimary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Location Card
                    AppleGroupedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(pal.surfaceElevated, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = pal.accentBlue,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Share Location",
                                        style = AppleHeadline,
                                        color = pal.textPrimary,
                                    )
                                    Text(
                                        text = if (shareLocation) "19.0760° N, 72.8777° E (±4m)" else "Location hidden",
                                        style = AppleFootnote,
                                        color = pal.textSecondary,
                                    )
                                }
                            }

                            Switch(
                                checked = shareLocation,
                                onCheckedChange = { shareLocation = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = pal.accentGreen,
                                    uncheckedThumbColor = pal.textMuted,
                                    uncheckedTrackColor = pal.surfaceElevated,
                                ),
                            )
                        }

                        AppleDivider()

                        // Notes Field
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicTextField(
                                value = note,
                                onValueChange = { note = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = AppleBody.copy(color = pal.textPrimary),
                                cursorBrush = SolidColor(pal.accentBlue),
                                decorationBox = { innerTextField ->
                                    if (note.isEmpty()) {
                                        Text(
                                            text = "Add details (injuries, floor, hazards)…",
                                            style = AppleBody,
                                            color = pal.textMuted,
                                        )
                                    }
                                    innerTextField()
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 3-Second Hold-to-Confirm Emergency SOS Button
                    HoldToSosButton(
                        onHoldComplete = {
                            viewModel.sendSos(
                                category = selectedCategory,
                                note = note,
                                shareLocation = shareLocation,
                            )
                        },
                        holdDurationMillis = 3000,
                    )

                    Spacer(Modifier.height(10.dp))

                    // Quick Simulation Trigger for Demoing
                    Surface(
                        onClick = {
                            viewModel.triggerSimulation(
                                category = selectedCategory,
                                note = if (note.isBlank()) "Asthma emergency, need inhaler" else note,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = pal.surfaceElevated,
                        border = BorderStroke(0.5.dp, pal.border),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Simulate 4-Node Relay (You → Aarav → Kabir → Priya)",
                                style = AppleFootnote,
                                color = pal.accentBlue,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                } else {
                    // LIVE ACTIVE BROADCAST STATE
                    val currentRoute = lastSos?.routeHistory ?: (if (activePeers.any { it.connected }) listOf("You", activePeers.first { it.connected }.name) else listOf("You"))
                    val isDelivered = lastSos?.status == DeliveryStatus.DELIVERED

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = pal.surface,
                        border = BorderStroke(1.dp, if (isDelivered) pal.accentGreen else pal.emergencyRed),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(if (isDelivered) pal.accentGreen else pal.emergencyRed, CircleShape)
                                            .border(4.dp, (if (isDelivered) pal.accentGreen else pal.emergencyRed).copy(alpha = pulseAlpha), CircleShape),
                                    )
                                    Text(
                                        text = if (isDelivered) "SOS DELIVERED (ACK)" else "SOS BROADCASTING",
                                        style = AppleHeadline,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDelivered) pal.accentGreen else pal.emergencyRed,
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = (if (isDelivered) pal.accentGreen else pal.emergencyRed).copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = if (isDelivered) "ACK CONFIRMED" else "CRITICAL",
                                        style = AppleCaption,
                                        color = if (isDelivered) pal.accentGreen else pal.emergencyRed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "Type: $selectedCategory Emergency",
                                style = AppleHeadline,
                                color = pal.textPrimary,
                            )
                            if (note.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "\"$note\"",
                                    style = AppleFootnote,
                                    color = pal.textSecondary,
                                )
                            }
                            if (shareLocation) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Location: 28.6139° N, 77.2090° E (Shared)",
                                    style = AppleCaption,
                                    color = pal.accentBlue,
                                )
                            }

                            Spacer(Modifier.height(18.dp))
                            AppleDivider()
                            Spacer(Modifier.height(14.dp))

                            // Live Multi-hop steps
                            Text(
                                text = "Relay Pathway (${currentRoute.size} nodes)",
                                style = AppleFootnote,
                                color = pal.textSecondary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(8.dp))

                            currentRoute.forEachIndexed { i, node ->
                                val stepLabel = when (i) {
                                    0 -> "Origin ($node)"
                                    currentRoute.size - 1 -> if (isDelivered) "Final Recipient ($node)" else "Hop $i: $node (Relaying)"
                                    else -> "Hop $i: $node (Relay)"
                                }
                                HopStep(stepLabel, if (i == 0) "Initiated" else "${i * 2}s ago", done = true)
                            }

                            Spacer(Modifier.height(16.dp))
                            HopMeter(reached = currentRoute.size.coerceAtMost(6), total = 6)

                            Spacer(Modifier.height(20.dp))

                            // Cancel / Resolve button
                            Button(
                                onClick = { viewModel.cancelSos() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = pal.surfaceElevated,
                                    contentColor = pal.textPrimary,
                                ),
                            ) {
                                Text(
                                    text = if (isDelivered) "Dismiss SOS Alert" else "Cancel SOS Broadcast",
                                    style = AppleSubhead,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))

                // Alert History
                SectionHeader(
                    title = "Alert History",
                    actionText = if (effectiveAlerts.isNotEmpty()) "Clear" else null,
                    onActionClick = { viewModel.clearAlertHistory() },
                )
                Spacer(Modifier.height(10.dp))

                AppleGroupedCard {
                    if (effectiveAlerts.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = pal.accentGreen,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "No emergency alerts recorded · All clear",
                                style = AppleFootnote,
                                color = pal.textMuted,
                            )
                        }
                    } else {
                        effectiveAlerts.forEachIndexed { index, alert ->
                        if (index > 0) AppleDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(pal.emergencyRed.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = pal.emergencyRed,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = alert.senderName,
                                        style = AppleHeadline,
                                        color = pal.textPrimary,
                                    )
                                    Text(
                                        text = alert.createdAt.asRelative(),
                                        style = AppleCaption,
                                        color = pal.textMuted,
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = alert.body,
                                    style = AppleFootnote,
                                    color = pal.textSecondary,
                                    maxLines = 1,
                                )
                                Spacer(Modifier.height(4.dp))
                                ModernRouteTrace(route = alert.routeHistory)
                            }
                        }
                    }
                }
            }
        }
    }
}