package com.sunless.lynq.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunless.lynq.ui.navigation.Screen
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.Lynq

private data class NavItemSpec(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val isEmergency: Boolean = false,
)

/**
 * Apple Floating Rounded Island Navigation Bar (Interactive Sliding Dock).
 *
 * Features:
 * - Fluid finger-scrub gesture: sliding your finger continuously across the dock
 *   smoothly drives the glowing indicator pill along the track with tactile haptic ticks.
 * - When released, automatically commits and switches to the destination screen.
 * - Tap support: 1-tap instantly springs the sliding pill to the target tab and navigates.
 * - Equal-sized uniform slots across all 5 navigation destinations.
 * - Adaptive pill color: morphs to emergency crimson when hovering over SOS,
 *   and Apple celestial blue for Network/Messages/Mesh/Settings.
 */
@Composable
fun CmdBar(
    currentRoute: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val pal = Lynq.pal
    val haptics = LocalHapticFeedback.current

    val items = remember {
        listOf(
            NavItemSpec("Network", Icons.Rounded.Sensors, Screen.Dashboard.route),
            NavItemSpec("Messages", Icons.Rounded.ChatBubbleOutline, Screen.Messages.route),
            NavItemSpec("SOS", Icons.Rounded.Emergency, Screen.Emergency.route, isEmergency = true),
            NavItemSpec("Mesh", Icons.Rounded.Hub, Screen.MeshExplorer.route),
            NavItemSpec("Settings", Icons.Rounded.Settings, Screen.Settings.route),
        )
    }

    val currentRouteState by rememberUpdatedState(currentRoute)
    val onSelectState by rememberUpdatedState(onSelect)

    var selectedRoute by remember(currentRoute) { mutableStateOf(currentRoute) }
    val committedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    val visualIndex = draggingIndex ?: committedIndex

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val barWidth = size.width.toFloat()
                            fun getIndexFromX(x: Float): Int {
                                val fraction = (x / barWidth).coerceIn(0f, 0.999f)
                                return (fraction * items.size).toInt().coerceIn(0, items.size - 1)
                            }

                            var activeIdx = getIndexFromX(down.position.x)
                            draggingIndex = activeIdx
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    // Touch released: commit navigation to the destination tab immediately
                                    val finalRoute = items[activeIdx].route
                                    selectedRoute = finalRoute
                                    draggingIndex = null
                                    if (finalRoute != currentRouteState) {
                                        onSelectState(finalRoute)
                                    }
                                    break
                                }

                                val newIdx = getIndexFromX(change.position.x)
                                if (newIdx != activeIdx) {
                                    activeIdx = newIdx
                                    draggingIndex = newIdx
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                change.consume()
                            }
                        }
                    }
                },
            shape = RoundedCornerShape(28.dp),
            color = pal.surface.copy(alpha = 0.94f), // Frosted Apple Secondary Background
            border = BorderStroke(0.5.dp, pal.border), // Apple delicate hairline
            shadowElevation = if (pal.isDark) 14.dp else 8.dp,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp), // Uniform 4dp margin on all sides for mathematical concentricity
            ) {
                val totalWidth = maxWidth
                val tabWidth = totalWidth / items.size

                // Smooth organic spring animation for sliding indicator pill
                val animatedIndex by animateFloatAsState(
                    targetValue = visualIndex.toFloat(),
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 500f,
                    ),
                    label = "slidingPillPosition",
                )

                // Smooth morphing pill background & border colors
                val currentVisualItem = items[visualIndex]
                val pillColor by animateColorAsState(
                    targetValue = if (currentVisualItem.isEmergency) {
                        pal.emergencyRed.copy(alpha = 0.22f)
                    } else {
                        pal.accentBlue.copy(alpha = 0.16f)
                    },
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "pillColorMorph",
                )

                val pillBorderColor by animateColorAsState(
                    targetValue = if (currentVisualItem.isEmergency) {
                        pal.emergencyRed.copy(alpha = 0.45f)
                    } else {
                        pal.accentBlue.copy(alpha = 0.35f)
                    },
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "pillBorderColorMorph",
                )

                // 1. Sliding Pill Background Indicator
                // Concentricity rule: R_inner = R_outer (28dp) - padding (4dp) = 24dp
                Box(
                    modifier = Modifier
                        .offset(x = tabWidth * animatedIndex)
                        .width(tabWidth)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(pillColor)
                        .border(
                            width = 0.5.dp,
                            color = pillBorderColor,
                            shape = RoundedCornerShape(24.dp),
                        ),
                )

                // 2. Navigation Tab Icons & Labels Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == visualIndex

                        val itemColor by animateColorAsState(
                            targetValue = when {
                                item.isEmergency && isSelected -> pal.emergencyRed
                                item.isEmergency -> pal.emergencyRed.copy(alpha = 0.85f)
                                isSelected -> pal.accentBlue
                                else -> pal.textSecondary
                            },
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                            label = "tabColor_$index",
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = itemColor,
                                    modifier = Modifier.size(21.dp),
                                )
                                Text(
                                    text = item.label,
                                    color = itemColor,
                                    style = AppleCaption,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}