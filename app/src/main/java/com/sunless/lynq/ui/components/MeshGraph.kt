package com.sunless.lynq.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.ui.theme.AppleCaption
import com.sunless.lynq.ui.theme.AppleFootnote
import com.sunless.lynq.ui.theme.AppleHeadline
import com.sunless.lynq.ui.theme.AppleSubhead
import com.sunless.lynq.ui.theme.Lynq
import com.sunless.lynq.util.asRelative
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Obsidian-Inspired Knowledge Graph Topology Canvas.
 *
 * Implements Obsidian's iconic Graph View aesthetic:
 * - Deep near-black charcoal atmosphere (`#0D0E12` to `#07080A`)
 * - Small circular celestial nodes with soft outer halo glows
 * - Delicate, whisper-thin semi-transparent connecting filaments
 * - Natural clustered node distribution (Local Cluster & East Sector in Demo, Dynamic Orbit in Live)
 * - Obsidian interactive focus: clicking a node highlights its connected sub-graph
 *   and smoothly dims unrelated nodes into the background
 * - Organic harmonic physics drift (Brownian motion like Obsidian's D3 force graph)
 * - Interactive Pinch-to-Zoom (0.5x to 3.0x) and fluid 2D Pan gestures
 * - Active SOS multi-hop energy stream with luminous packet travel
 */
@Composable
fun MeshGraph(
    peers: List<Peer>,
    modifier: Modifier = Modifier,
    liveRoute: List<String> = emptyList(),
    isDemoMode: Boolean = false,
    height: Dp? = null,
    interactive: Boolean = true,
    selectedNodeName: String? = null,
    onNodeSelected: ((String?) -> Unit)? = null,
    onOpenMessage: ((String) -> Unit)? = null,
) {
    val pal = Lynq.pal
    val textMeasurer = rememberTextMeasurer()

    // Flag indicating whether to render the signature demo cluster (Aarav, Kabir, Meera, Priya)
    val isDemo = isDemoMode

    // Transform State (Pan & Pinch-to-Zoom)
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var internalSelectedNode by remember { mutableStateOf<String?>(selectedNodeName) }

    // Sync external selected state if updated
    val activeSelection = selectedNodeName ?: internalSelectedNode

    // Gentle Obsidian harmonic drift physics
    val infiniteTransition = rememberInfiniteTransition(label = "obsidianDrift")
    val floatTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "driftAngle",
    )

    // Breathing glow for SOS active route & selected node
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlow",
    )

    // Luminous packet pulse along live route
    val packetFlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "packetFlow",
    )

    // Connectivity & cluster graph definition
    val neighborMap = remember(peers, isDemo) {
        if (isDemo) {
            mapOf(
                "You" to setOf("Aarav", "Meera"),
                "Aarav" to setOf("You", "Meera", "Kabir"),
                "Meera" to setOf("You", "Aarav"),
                "Kabir" to setOf("Aarav", "Priya"),
                "Priya" to setOf("Kabir"),
            )
        } else {
            val map = mutableMapOf<String, MutableSet<String>>()
            map["You"] = mutableSetOf()
            for (p in peers) {
                map.getOrPut("You") { mutableSetOf() }.add(p.name)
                map.getOrPut(p.name) { mutableSetOf() }.add("You")
            }
            map.mapValues { it.value.toSet() }
        }
    }

    val isDark = pal.isDark
    val graphBg = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF111319), // Obsidian charcoal inner ambient
                Color(0xFF07080B), // Deep void outer base
            ),
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF), // Crisp light inner ambient
                Color(0xFFECECF2), // Soft Apple slate outer base
            ),
        )
    }

    val boxModifier = modifier
        .fillMaxWidth()
        .then(if (height != null) Modifier.height(height) else Modifier.fillMaxSize())
        .clip(RoundedCornerShape(24.dp))
        .background(graphBg)
        .border(0.5.dp, pal.border, RoundedCornerShape(24.dp))

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (interactive) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 3.0f)
                                    panOffset += pan
                                }
                            }
                            .pointerInput(scale, panOffset) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val center = Offset(w * 0.48f, h * 0.48f)

                                    // Transform tap coordinate to graph world space
                                    val worldTap = center + (tapOffset - panOffset - center) / scale

                                    // Base positions for hit-test
                                    val posMap = computeObsidianPositions(w, h, floatTime, peers, isDemo)

                                    var tappedNode: String? = null
                                    var minDistance = Float.MAX_VALUE
                                    val hitRadius = (36.dp.toPx() / scale).coerceAtLeast(28.dp.toPx())

                                    for ((name, pos) in posMap) {
                                        val dx = worldTap.x - pos.x
                                        val dy = worldTap.y - pos.y
                                        val dist = sqrt(dx * dx + dy * dy)
                                        if (dist <= hitRadius && dist < minDistance) {
                                            minDistance = dist
                                            tappedNode = name
                                        }
                                    }

                                    val newSelection = if (internalSelectedNode == tappedNode) null else tappedNode
                                    internalSelectedNode = newSelection
                                    onNodeSelected?.invoke(newSelection)
                                }
                            }
                    } else Modifier,
                ),
        ) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.48f, h * 0.48f)

            // 1. Ambient Celestial Micro-Dust (Obsidian deep canvas feeling)
            val dustList = listOf(
                Offset(w * 0.08f, h * 0.16f),
                Offset(w * 0.91f, h * 0.14f),
                Offset(w * 0.84f, h * 0.85f),
                Offset(w * 0.14f, h * 0.82f),
                Offset(w * 0.54f, h * 0.10f),
                Offset(w * 0.36f, h * 0.92f),
                Offset(w * 0.72f, h * 0.36f),
                Offset(w * 0.22f, h * 0.48f),
            )
            dustList.forEach { pt ->
                drawCircle(
                    color = if (isDark) Color.White.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.04f),
                    radius = 0.9.dp.toPx(),
                    center = pt,
                )
            }

            // Apply Pan & Pinch-to-Zoom matrix
            translate(panOffset.x, panOffset.y) {
                scale(scale, pivot = center) {

                    // 2. Compute Natural Clustered Node Positions with Harmonic Drift
                    val posMap = computeObsidianPositions(w, h, floatTime, peers, isDemo)
                    val youPos = posMap["You"] ?: center

                    // 3. Define Natural Constellation Graph Edges
                    data class ObsidianEdge(
                        val fromName: String,
                        val toName: String,
                        val from: Offset,
                        val to: Offset,
                        val isDirect: Boolean,
                    )

                    val edges = mutableListOf<ObsidianEdge>()
                    if (isDemo) {
                        val aaravPos = posMap["Aarav"] ?: center
                        val meeraPos = posMap["Meera"] ?: center
                        val kabirPos = posMap["Kabir"] ?: center
                        val priyaPos = posMap["Priya"] ?: center

                        edges.add(ObsidianEdge("You", "Aarav", youPos, aaravPos, isDirect = true))
                        edges.add(ObsidianEdge("You", "Meera", youPos, meeraPos, isDirect = true))
                        edges.add(ObsidianEdge("Meera", "Aarav", meeraPos, aaravPos, isDirect = false))
                        edges.add(ObsidianEdge("Aarav", "Kabir", aaravPos, kabirPos, isDirect = false)) // Bridge
                        edges.add(ObsidianEdge("Kabir", "Priya", kabirPos, priyaPos, isDirect = false))
                    } else {
                        for (peer in peers) {
                            val peerPos = posMap[peer.name] ?: continue
                            if (peer.hops <= 1 || peer.connected) {
                                edges.add(ObsidianEdge("You", peer.name, youPos, peerPos, isDirect = true))
                            } else {
                                val relayPeer = peers.firstOrNull { it.relaying && it.name != peer.name }?.name ?: "You"
                                val relayPos = posMap[relayPeer] ?: youPos
                                edges.add(ObsidianEdge(relayPeer, peer.name, relayPos, peerPos, isDirect = false))
                            }
                        }
                    }

                    // Ambient radar ripples if Live mode and scanning with no peers yet
                    if (!isDemo && peers.isEmpty()) {
                        val radarRadius1 = (35.dp.toPx() + (packetFlow * 85.dp.toPx()))
                        val radarAlpha1 = (1f - packetFlow) * 0.35f
                        drawCircle(
                            color = pal.accentBlue.copy(alpha = radarAlpha1),
                            radius = radarRadius1,
                            center = youPos,
                            style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
                        )
                        val progress2 = (packetFlow + 0.5f) % 1f
                        val radarRadius2 = (15.dp.toPx() + (progress2 * 85.dp.toPx()))
                        val radarAlpha2 = (1f - progress2) * 0.25f
                        drawCircle(
                            color = pal.accentBlue.copy(alpha = radarAlpha2),
                            radius = radarRadius2,
                            center = youPos,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }

                    // Helper to determine if an edge is on the live SOS route
                    val isEdgeOnRoute = { e: ObsidianEdge ->
                        var onRoute = false
                        if (liveRoute.size >= 2) {
                            for (i in 0 until liveRoute.size - 1) {
                                val n1 = liveRoute[i]
                                val n2 = liveRoute[i + 1]
                                if ((n1.equals(e.fromName, true) && n2.equals(e.toName, true)) ||
                                    (n2.equals(e.fromName, true) && n1.equals(e.toName, true))
                                ) {
                                    onRoute = true
                                    break
                                }
                            }
                        }
                        onRoute
                    }

                    // Helper to calculate Obsidian node focus opacity
                    val getNodeOpacity = { name: String ->
                        if (activeSelection == null) {
                            1.0f
                        } else if (name.equals(activeSelection, true) ||
                            neighborMap[activeSelection]?.contains(name) == true
                        ) {
                            1.0f
                        } else {
                            0.20f // Dimmed into the cosmic negative space
                        }
                    }

                    // 4. Draw Constellation Filaments (Delicate & Hairline)
                    edges.forEach { edge ->
                        val onLiveRoute = isEdgeOnRoute(edge)
                        val isConnectedToSelection = activeSelection == null ||
                            (edge.fromName.equals(activeSelection, true) || edge.toName.equals(activeSelection, true))

                        val edgeOpacity = when {
                            activeSelection != null && !isConnectedToSelection -> 0.08f
                            activeSelection != null && isConnectedToSelection -> 0.90f
                            else -> 0.28f
                        }

                        if (onLiveRoute) {
                            // Active SOS Multi-Hop Crimson Stream
                            // Soft ambient halo
                            drawLine(
                                color = pal.emergencyRed.copy(alpha = 0.22f * edgeOpacity),
                                start = edge.from,
                                end = edge.to,
                                strokeWidth = 3.6f,
                                cap = StrokeCap.Round,
                            )
                            // Core crisp filament
                            drawLine(
                                color = pal.emergencyRed.copy(alpha = 0.85f * edgeOpacity),
                                start = edge.from,
                                end = edge.to,
                                strokeWidth = 1.6f,
                                cap = StrokeCap.Round,
                            )

                            // Animated Luminous Energy Photon
                            val packetPos = Offset(
                                edge.from.x + (edge.to.x - edge.from.x) * packetFlow,
                                edge.from.y + (edge.to.y - edge.from.y) * packetFlow,
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = edgeOpacity),
                                radius = 2.4.dp.toPx(),
                                center = packetPos,
                            )
                        } else {
                            // Delicate Constellation Hairline Filament
                            val filamentColor = if (edge.isDirect) {
                                if (isDark) Color(0xFF6BA3FF).copy(alpha = edgeOpacity)
                                else pal.accentBlue.copy(alpha = edgeOpacity * 0.85f)
                            } else {
                                if (isDark) Color(0xFFC7C7CC).copy(alpha = edgeOpacity * 0.75f)
                                else Color(0xFF8E8E93).copy(alpha = edgeOpacity * 0.60f)
                            }
                            val strokeWidth = if (edge.isDirect) 1.2f else 0.9f
                            val pathEffect = if (!edge.isDirect) PathEffect.dashPathEffect(floatArrayOf(3.5f, 3.5f)) else null

                            drawLine(
                                color = filamentColor,
                                start = edge.from,
                                end = edge.to,
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round,
                                pathEffect = pathEffect,
                            )
                        }
                    }

                    // 5. Draw Obsidian Circular Nodes (Small, atmospheric, soft halo)
                    data class ObsidianNode(
                        val name: String,
                        val pos: Offset,
                        val isYou: Boolean,
                        val isRelay: Boolean,
                        val onRoute: Boolean,
                        val isHub: Boolean,
                    )

                    val nodes = mutableListOf<ObsidianNode>()
                    nodes.add(ObsidianNode("You", youPos, isYou = true, isRelay = false, onRoute = liveRoute.any { it.equals("You", true) }, isHub = true))

                    if (isDemo) {
                        val aaravPos = posMap["Aarav"] ?: center
                        val meeraPos = posMap["Meera"] ?: center
                        val kabirPos = posMap["Kabir"] ?: center
                        val priyaPos = posMap["Priya"] ?: center

                        nodes.add(ObsidianNode("Aarav", aaravPos, isYou = false, isRelay = true, onRoute = liveRoute.any { it.equals("Aarav", true) }, isHub = true))
                        nodes.add(ObsidianNode("Meera", meeraPos, isYou = false, isRelay = true, onRoute = liveRoute.any { it.equals("Meera", true) }, isHub = false))
                        nodes.add(ObsidianNode("Kabir", kabirPos, isYou = false, isRelay = false, onRoute = liveRoute.any { it.equals("Kabir", true) }, isHub = true))
                        nodes.add(ObsidianNode("Priya", priyaPos, isYou = false, isRelay = false, onRoute = liveRoute.any { it.equals("Priya", true) }, isHub = false))
                    } else {
                        for (peer in peers) {
                            val pPos = posMap[peer.name] ?: continue
                            nodes.add(
                                ObsidianNode(
                                    name = peer.name,
                                    pos = pPos,
                                    isYou = false,
                                    isRelay = peer.relaying,
                                    onRoute = liveRoute.any { it.equals(peer.name, true) },
                                    isHub = peer.relaying || peer.hops > 1,
                                )
                            )
                        }
                    }

                    nodes.forEach { node ->
                        val nodeOpacity = getNodeOpacity(node.name)
                        val isSelected = activeSelection?.equals(node.name, true) == true

                        val baseColor = when {
                            node.isYou -> if (isDark) Color(0xFF4B96FF) else Color(0xFF007AFF) // Stellar Blue
                            node.onRoute -> pal.emergencyRed
                            node.isRelay -> if (isDark) Color(0xFF32D74B) else Color(0xFF34C759) // Mint / Apple Green
                            else -> if (isDark) Color(0xFFC7C7CC) else Color(0xFF8E8E93) // Peer Gray
                        }

                        // Size hierarchy: Hubs (6.5dp), Leaves (5.0dp), You (7.5dp)
                        val baseRadius = when {
                            node.isYou -> 7.2.dp.toPx()
                            node.isHub -> 6.2.dp.toPx()
                            else -> 4.8.dp.toPx()
                        }

                        // Outer Soft Glow Halos (Subtle & atmospheric)
                        val outerHaloRadius = baseRadius * (if (isSelected) 2.6f else 2.1f)
                        val innerHaloRadius = baseRadius * (if (isSelected) 1.8f else 1.4f)

                        val haloAlpha = when {
                            node.onRoute -> pulseGlow * 0.35f * nodeOpacity
                            isSelected -> 0.40f * nodeOpacity
                            else -> 0.12f * nodeOpacity
                        }

                        // Outer diffuse aura
                        drawCircle(
                            color = baseColor.copy(alpha = haloAlpha * 0.5f),
                            radius = outerHaloRadius,
                            center = node.pos,
                        )

                        // Inner soft glow
                        drawCircle(
                            color = baseColor.copy(alpha = haloAlpha),
                            radius = innerHaloRadius,
                            center = node.pos,
                        )

                        // Crisp Node Rim (Separation backing)
                        drawCircle(
                            color = if (isDark) Color(0xFF090A0D) else Color(0xFFFFFFFF),
                            radius = baseRadius + 1.4.dp.toPx(),
                            center = node.pos,
                        )

                        // Solid Core Celestial Disk (Pure solid color)
                        drawCircle(
                            color = baseColor.copy(alpha = nodeOpacity),
                            radius = baseRadius,
                            center = node.pos,
                        )

                        // Refined Obsidian Typography (Clean sans-serif note label)
                        val labelColor = when {
                            node.onRoute -> pal.emergencyRed.copy(alpha = nodeOpacity)
                            node.isYou -> pal.textPrimary.copy(alpha = nodeOpacity)
                            isSelected -> pal.textPrimary.copy(alpha = nodeOpacity)
                            else -> pal.textSecondary.copy(alpha = nodeOpacity * 0.90f)
                        }

                        val labelStyle = TextStyle(
                            color = labelColor,
                            fontSize = if (node.isYou || isSelected) 11.sp else 10.sp,
                            fontWeight = if (node.isYou || isSelected || node.onRoute) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.25.sp,
                        )

                        val measured = textMeasurer.measure(node.name, style = labelStyle)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = node.name,
                            topLeft = Offset(
                                node.pos.x + baseRadius + 5.dp.toPx(),
                                node.pos.y - (measured.size.height / 2f),
                            ),
                            style = labelStyle,
                        )
                    }
                }
            }
        }

        // Minimalist Obsidian Floating Canvas Chrome (Controls & HUD)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top HUD Row: Minimal Title Pill & Camera Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Subtle Status Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = pal.surface.copy(alpha = 0.92f),
                    border = BorderStroke(0.5.dp, pal.border),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val statusColor = when {
                            isDemo -> Color(0xFFAF52DE)
                            peers.isEmpty() -> pal.accentBlue
                            else -> pal.accentGreen
                        }
                        val statusText = when {
                            activeSelection != null -> "Focus: $activeSelection"
                            isDemo -> "Demo Simulation · 4 Nodes"
                            peers.isEmpty() -> "Scanning · Bluetooth & Wi-Fi"
                            else -> "${peers.size} Live Device${if (peers.size > 1) "s" else ""} Online"
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(statusColor, CircleShape),
                        )
                        Text(
                            text = statusText,
                            style = AppleCaption,
                            fontSize = 10.5.sp,
                            color = pal.textSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Minimal Icon Controls
                if (interactive) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = pal.surface.copy(alpha = 0.92f),
                            border = BorderStroke(0.5.dp, pal.border),
                            modifier = Modifier.size(30.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    panOffset = Offset.Zero
                                    internalSelectedNode = null
                                    onNodeSelected?.invoke(null)
                                },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CenterFocusStrong,
                                    contentDescription = "Center Focus",
                                    tint = pal.textSecondary,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Floating Bar: Inspector or Minimal Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Animated Floating Inspector when node is selected
                AnimatedVisibility(
                    visible = activeSelection != null,
                    enter = fadeIn(tween(160)),
                    exit = fadeOut(tween(140)),
                ) {
                    val peer = peers.find { it.name.equals(activeSelection, true) }
                    val isYou = activeSelection.equals("You", true)
                    val isDirect = isYou || peer?.hops == 1

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = pal.surface.copy(alpha = 0.95f),
                        border = BorderStroke(0.5.dp, pal.border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = activeSelection ?: "",
                                        style = AppleHeadline,
                                        fontWeight = FontWeight.SemiBold,
                                        color = pal.textPrimary,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDirect) pal.accentBlue.copy(alpha = 0.2f) else pal.accentGreen.copy(alpha = 0.2f),
                                    ) {
                                        Text(
                                            text = if (isYou) "Local Device" else if (isDirect) "Direct link" else "${peer?.hops ?: 2} hops relay",
                                            style = AppleCaption,
                                            fontSize = 9.5.sp,
                                            color = if (isDirect) pal.accentBlue else pal.accentGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = if (isYou) "P2P_CLUSTER Coordinator" else "${peer?.id?.take(8) ?: "Node"} · Seen ${peer?.lastSeen?.asRelative() ?: "just now"}",
                                    style = AppleFootnote,
                                    fontSize = 11.sp,
                                    color = pal.textMuted,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!isYou && onOpenMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = pal.accentBlue.copy(alpha = 0.18f),
                                        border = BorderStroke(0.5.dp, pal.accentBlue.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { onOpenMessage(peer?.id ?: peer?.name ?: activeSelection ?: "") },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                                contentDescription = "Message",
                                                tint = pal.accentBlue,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Text(
                                                text = "Chat",
                                                style = AppleCaption,
                                                fontWeight = FontWeight.Medium,
                                                color = pal.accentBlue,
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = pal.surfaceElevated,
                                    modifier = Modifier.size(26.dp),
                                ) {
                                    IconButton(
                                        onClick = {
                                            internalSelectedNode = null
                                            onNodeSelected?.invoke(null)
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss",
                                            tint = pal.textSecondary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Minimal Obsidian Bottom Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ObsidianLegendDot(color = if (isDark) Color(0xFF4B96FF) else Color(0xFF007AFF), label = "You")
                        ObsidianLegendDot(color = if (isDark) Color(0xFF32D74B) else Color(0xFF34C759), label = "Relay")
                        ObsidianLegendDot(color = if (isDark) Color(0xFFC7C7CC) else Color(0xFF8E8E93), label = "Peer")
                        if (liveRoute.isNotEmpty()) {
                            ObsidianLegendDot(color = pal.emergencyRed, label = "SOS Path")
                        }
                    }

                    Text(
                        text = "pinch & drag canvas",
                        style = AppleCaption,
                        fontSize = 10.sp,
                        color = pal.textMuted,
                    )
                }
            }
        }
    }
}

/**
 * Calculates organic cluster distribution with subtle harmonic physics drift.
 * In Demo mode, renders the signature 5-node clustered topology (You, Aarav, Meera, Kabir, Priya).
 * In Live mode, dynamically positions actual physical devices discovered and connected.
 */
private fun computeObsidianPositions(
    w: Float,
    h: Float,
    floatTime: Float,
    peers: List<Peer>,
    isDemo: Boolean,
): Map<String, Offset> {
    val cx = w * 0.48f
    val cy = h * 0.48f

    val isTallScreen = h > w * 1.15f
    val scaleFactor = if (isTallScreen) 1.20f else 1.0f

    val effectiveH = minOf(h, w * 0.642f) * scaleFactor
    val effectiveW = minOf(w, h * 1.55f) * scaleFactor

    // Harmonic drifts (Brownian motion)
    val d0 = Offset(sin(floatTime) * 2.5f, cos(floatTime) * 2.0f)
    val d1 = Offset(cos(floatTime * 0.85f) * 3.2f, sin(floatTime * 0.85f) * 2.5f)
    val d2 = Offset(-sin(floatTime * 1.05f) * 2.8f, cos(floatTime * 1.05f) * 2.8f)
    val d3 = Offset(cos(floatTime * 0.95f) * 3.5f, -sin(floatTime * 0.95f) * 2.4f)
    val d4 = Offset(-cos(floatTime * 1.1f) * 2.6f, -sin(floatTime * 1.1f) * 3.0f)

    if (isDemo) {
        // Cluster 1 (Local): You (center-left), Meera (upper-left), Aarav (bridge-center)
        val youPos = Offset(cx - effectiveW * 0.16f, cy + effectiveH * 0.04f) + d0
        val meeraPos = Offset(cx - effectiveW * 0.32f, cy - effectiveH * 0.18f) + d2
        val aaravPos = Offset(cx + effectiveW * 0.04f, cy - effectiveH * 0.20f) + d1

        // Cluster 2 (East Sector): Kabir (center-right hub), Priya (lower-right)
        val kabirPos = Offset(cx + effectiveW * 0.28f, cy + effectiveH * 0.08f) + d3
        val priyaPos = Offset(cx + effectiveW * 0.20f, cy + effectiveH * 0.30f) + d4

        return mapOf(
            "You" to youPos,
            "Aarav" to aaravPos,
            "Meera" to meeraPos,
            "Kabir" to kabirPos,
            "Priya" to priyaPos,
        )
    }

    // Dynamic Live Hardware Mesh
    if (peers.isEmpty()) {
        return mapOf("You" to Offset(cx, cy) + d0)
    }

    val map = mutableMapOf<String, Offset>()
    val youPos = Offset(cx - effectiveW * 0.22f, cy) + d0
    map["You"] = youPos

    when (peers.size) {
        1 -> {
            val p0 = peers[0]
            val p0Pos = Offset(cx + effectiveW * 0.22f, cy) + d1
            map[p0.name] = p0Pos
        }
        2 -> {
            val p0 = peers[0]
            val p1 = peers[1]
            val p0Pos = Offset(cx + effectiveW * 0.22f, cy - effectiveH * 0.24f) + d1
            val p1Pos = Offset(cx + effectiveW * 0.22f, cy + effectiveH * 0.24f) + d2
            map[p0.name] = p0Pos
            map[p1.name] = p1Pos
        }
        else -> {
            val count = peers.size
            for (i in 0 until count) {
                val peer = peers[i]
                val fraction = if (count > 1) i.toFloat() / (count - 1) else 0.5f
                val angle = -0.95f + fraction * 1.9f // Arc between ~ -55 and +55 deg
                val dist = effectiveW * 0.36f + (if (peer.hops > 1) effectiveW * 0.14f else 0f)
                val drift = Offset(
                    cos(floatTime * 0.9f + i * 1.3f) * 3f,
                    sin(floatTime * 0.8f + i * 1.3f) * 2.5f,
                )
                val pos = Offset(
                    youPos.x + cos(angle) * dist,
                    youPos.y + sin(angle) * dist,
                ) + drift
                map[peer.name] = pos
            }
        }
    }

    return map
}

@Composable
private fun ObsidianLegendDot(color: Color, label: String) {
    val pal = Lynq.pal
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            color = pal.textSecondary,
            style = AppleCaption,
            fontSize = 10.sp,
        )
    }
}
