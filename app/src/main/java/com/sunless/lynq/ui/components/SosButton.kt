package com.sunless.lynq.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.ui.theme.AppleBody
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.Lynq

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 3-Second Hold-to-Confirm Emergency SOS Button.
 * Requires user to continuously press and hold for 3 seconds to prevent accidental sends.
 * Features an animated circular arc, horizontal radiant fill, and press compression.
 */
@Composable
fun HoldToSosButton(
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMillis: Int = 3000,
) {
    val pal = Lynq.pal
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isHolding) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "sosButtonScale",
    )

    val transition = rememberInfiniteTransition(label = "holdSosPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    val secondsRemaining = ((1f - progress.value) * (holdDurationMillis / 1000f)).coerceAtLeast(0f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val job = scope.launch {
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = holdDurationMillis,
                                    easing = LinearEasing,
                                ),
                            )
                            // 3 full seconds reached! Trigger SOS
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHoldComplete()
                            progress.snapTo(0f)
                            isHolding = false
                        }
                        try {
                            awaitRelease()
                        } finally {
                            job.cancel()
                            isHolding = false
                            scope.launch {
                                progress.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
                            }
                        }
                    },
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = pal.emergencyRed,
        border = BorderStroke(
            width = if (isHolding) 1.5.dp else 1.dp,
            color = if (isHolding) Color.White.copy(alpha = pulseAlpha) else pal.emergencyRed,
        ),
        shadowElevation = if (isHolding) 10.dp else 4.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 1. Radiant Background Progress Fill (Left to Right)
            if (progress.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.value)
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.35f),
                                ),
                            ),
                        ),
                )
            }

            // 2. Button Content (Circular Arc + Typography)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Tactile Circular 360-degree countdown arc
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val strokeWidth = 3.dp.toPx()
                        // Inactive background track
                        drawCircle(
                            color = Color.White.copy(alpha = if (isHolding) 0.30f else 0.18f),
                            style = Stroke(width = strokeWidth),
                        )
                        // Active charging arc
                        if (progress.value > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = progress.value * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Rounded.Emergency,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHolding) "Hold for SOS" else "Hold 3s for SOS",
                        style = AppleHeadline,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (isHolding) {
                            "Release to cancel · %.1fs remaining".format(secondsRemaining)
                        } else {
                            "Press and hold to broadcast across offline mesh"
                        },
                        style = AppleFootnote,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
            }
        }
    }
}

/**
 * Apple Minimalist Emergency SOS Action Trigger.
 * Designed like Apple's Satellite Emergency SOS.
 * Never blocked by network, AI, or loading states.
 */
@Composable
fun SosLever(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Emergency SOS",
    supporting: String = "Broadcast alert to all nearby nodes across mesh hops",
) {
    val pal = Lynq.pal
    val transition = rememberInfiniteTransition(label = "sosPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = pal.surface,
        border = BorderStroke(0.5.dp, pal.border),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            pal.emergencyRed.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Apple Red Action Pill
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(pal.emergencyRed, CircleShape)
                        .border(3.dp, pal.emergencyRed.copy(alpha = pulseAlpha), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Emergency,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = AppleHeadline,
                        fontWeight = FontWeight.Bold,
                        color = pal.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = supporting,
                        style = AppleFootnote,
                        color = pal.textSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Apple Style Hop Budget Meter (e.g. 3 of 6 hops).
 */
@Composable
fun HopMeter(
    reached: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal
    val progress = (reached.toFloat() / total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hop Limit",
                style = AppleFootnote,
                color = pal.textSecondary,
            )
            Text(
                text = "$reached of $total hops used",
                style = AppleCaption,
                color = pal.accentBlue,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = pal.accentBlue,
            trackColor = pal.surfaceElevated,
        )
    }
}

/**
 * Apple Style Hop Step in the multi-hop transmission timeline.
 */
@Composable
fun HopStep(
    name: String,
    time: String?,
    done: Boolean,
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    if (done) pal.accentGreen else pal.surfaceElevated,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(pal.textMuted, CircleShape),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = AppleSubhead,
                fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
                color = if (done) pal.textPrimary else pal.textMuted,
            )
        }

        if (time != null) {
            Text(
                text = time,
                style = AppleCaption,
                color = pal.textMuted,
            )
        } else {
            Text(
                text = "Relaying…",
                style = AppleCaption,
                color = pal.warningAmber,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun DeliveryTag(status: DeliveryStatus) = StatusChip(status = status)