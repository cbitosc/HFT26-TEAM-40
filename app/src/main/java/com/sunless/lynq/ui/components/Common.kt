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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.Priority
import com.sunless.lynq.ui.theme.AppleBody
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleLargeTitle
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.AppleTitle2
import com.sunless.lynq.ui.theme.Lynq
import com.sunless.lynq.ui.theme.TechCodeBoldStyle
import com.sunless.lynq.ui.theme.TechCodeStyle

// ---------------- Apple Minimal Top Bar ----------------

@Composable
fun TopBar(
    reachableCount: Int,
    online: Boolean = true,
    modifier: Modifier = Modifier,
    title: String = "Lynq",
) {
    val pal = Lynq.pal
    val transition = rememberInfiniteTransition(label = "beacon")
    val beaconAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "beaconAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Apple Wordmark / Large Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = AppleLargeTitle,
                color = pal.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        // Apple Dynamic Island Style Status Capsule
        Surface(
            shape = RoundedCornerShape(100.dp),
            color = pal.surface,
            border = BorderStroke(0.5.dp, pal.border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            color = if (online) pal.accentGreen.copy(alpha = beaconAlpha) else pal.textMuted,
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = if (online) "$reachableCount online" else "Offline",
                    style = AppleCaption,
                    color = if (online) pal.textPrimary else pal.textMuted,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ---------------- Apple Grouped Surface (Card) ----------------

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Lynq.pal.border,
    backgroundColor: Color = Lynq.pal.surface,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(0.5.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

// ---------------- Apple Inset Grouped Section ----------------

@Composable
fun AppleGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Lynq.pal.surface,
        border = BorderStroke(0.5.dp, Lynq.pal.border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
fun AppleDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = Lynq.pal.borderSubtle,
    )
}

// ---------------- Apple Metric Stat Card ----------------

@Composable
fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Lynq.pal.accentBlue,
    icon: ImageVector? = null,
) {
    val pal = Lynq.pal
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = pal.surface,
        border = BorderStroke(0.5.dp, pal.border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = AppleFootnote,
                    color = pal.textSecondary,
                    fontWeight = FontWeight.Medium,
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = AppleTitle2,
                fontWeight = FontWeight.Bold,
                color = pal.textPrimary,
            )
        }
    }
}

// ---------------- Apple Section Header ----------------

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val pal = Lynq.pal
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AppleHeadline,
            fontWeight = FontWeight.SemiBold,
            color = pal.textPrimary,
        )
        if (actionText != null) {
            Text(
                text = actionText,
                style = AppleFootnote,
                color = pal.accentBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .then(if (onActionClick != null) Modifier.clickable(onClick = onActionClick) else Modifier)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

// ---------------- Apple Status Chips ----------------

@Composable
fun StatusChip(
    status: DeliveryStatus,
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal
    val (label, bg, fg, icon) = when (status) {
        DeliveryStatus.DELIVERED -> Quadruple("Delivered", pal.accentGreen.copy(alpha = 0.15f), pal.accentGreen, Icons.Rounded.DoneAll)
        DeliveryStatus.RELAYED -> Quadruple("Relayed", pal.accentBlue.copy(alpha = 0.15f), pal.accentBlue, Icons.Rounded.Hub)
        DeliveryStatus.SENT -> Quadruple("Sent", pal.surfaceElevated, pal.textSecondary, Icons.Rounded.Check)
        DeliveryStatus.PENDING -> Quadruple("Queued", pal.warningAmber.copy(alpha = 0.15f), pal.warningAmber, Icons.Rounded.Schedule)
        DeliveryStatus.FAILED, DeliveryStatus.EXPIRED ->
            Quadruple("Failed", pal.emergencyRed.copy(alpha = 0.15f), pal.emergencyRed, Icons.Rounded.ErrorOutline)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = label,
                style = AppleCaption,
                color = fg,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal
    val (label, bg, fg) = when (priority) {
        Priority.CRITICAL -> Triple("SOS", pal.emergencyRed.copy(alpha = 0.18f), pal.emergencyRed)
        Priority.HIGH -> Triple("Urgent", pal.warningAmber.copy(alpha = 0.18f), pal.warningAmber)
        Priority.NORMAL -> Triple("Normal", pal.surfaceElevated, pal.textMuted)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = bg,
    ) {
        Text(
            text = label,
            style = AppleCaption,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
        )
    }
}

// ---------------- Apple Route Trail ----------------

@Composable
fun ModernRouteTrace(
    route: List<String>,
    modifier: Modifier = Modifier,
) {
    val pal = Lynq.pal
    if (route.isEmpty()) {
        Text(
            text = "Direct connection",
            style = AppleFootnote,
            color = pal.textMuted,
            modifier = modifier,
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        route.forEachIndexed { idx, hop ->
            if (idx > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = pal.textMuted,
                    modifier = Modifier.size(10.dp),
                )
            }
            val isFinal = idx == route.lastIndex
            Text(
                text = hop,
                style = AppleFootnote,
                color = if (isFinal) pal.accentBlue else pal.textSecondary,
                fontWeight = if (isFinal) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

// ---------------- Apple Style Avatar ----------------

@Composable
fun UserAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    isRelay: Boolean = false,
    isCritical: Boolean = false,
) {
    val pal = Lynq.pal
    val initial = name.take(1).uppercase()

    val bgColor = when {
        isCritical -> pal.emergencyRed
        isRelay -> pal.accentBlue
        else -> pal.surfaceElevated
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = AppleHeadline,
            fontWeight = FontWeight.Bold,
            color = if (isCritical || isRelay) Color.White else pal.textPrimary,
        )
    }
}