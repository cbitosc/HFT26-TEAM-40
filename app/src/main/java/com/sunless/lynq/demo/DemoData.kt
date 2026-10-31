package com.sunless.lynq.demo

import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.LynqMessage
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.domain.model.Priority
import java.util.UUID

/**
 * TEMPORARY demo content so the UI can be built and judged before the engine lands.
 *
 * Every screen takes its data as parameters, so replacing this object with real
 * `Flow`s from [com.sunless.lynq.engine] later is a wiring change, not a rewrite.
 * Delete this file when the router is live.
 */
object DemoData {

    const val MY_NAME = "Sunless"
    const val MY_DEVICE = "Pixel 8"
    const val MY_ID = "9f3c41d8 a7e5 4b02"

    val peers: List<Peer> = listOf(
        Peer(id = "2b7f9c41", name = "Aarav", hops = 1, relaying = true, lastSeen = now() - 4_000),
        Peer(id = "5d80e2aa", name = "Meera", hops = 1, relaying = true, lastSeen = now() - 11_000),
        Peer(id = "c14b76f3", name = "Kabir", hops = 2, lastSeen = now() - 38_000),
        Peer(id = "8ae0d915", name = "Priya", hops = 3, lastSeen = now() - 96_000),
    )

    val relayCount: Int get() = peers.count { it.relaying }
    val reachableCount: Int get() = peers.count { it.connected }

    /** Outgoing messages waiting for a connection — shown as the "Queued" stat. */
    val pendingMessages: List<LynqMessage> = listOf(
        LynqMessage(
            messageId = "q1",
            type = MessageType.CHAT,
            priority = Priority.NORMAL,
            senderId = MY_ID,
            senderName = "You",
            recipientId = "8ae0d915",
            body = "Staying near the east exit, we'll move at dawn.",
            createdAt = now() - 12 * 60_000,
            status = DeliveryStatus.PENDING,
        ),
        LynqMessage(
            messageId = "q2",
            type = MessageType.CHAT,
            priority = Priority.NORMAL,
            senderId = MY_ID,
            senderName = "You",
            recipientId = "c14b76f3",
            body = "Keep the app open, we'll relay you if the network thins out.",
            createdAt = now() - 47 * 60_000,
            status = DeliveryStatus.PENDING,
        ),
        LynqMessage(
            messageId = "q3",
            type = MessageType.CHAT,
            priority = Priority.NORMAL,
            senderId = MY_ID,
            senderName = "You",
            recipientId = "2b7f9c41",
            body = "Water is at the north gate if you can reach it.",
            createdAt = now() - 83 * 60_000,
            status = DeliveryStatus.PENDING,
        ),
    )

    data class Thread(
        val peer: Peer,
        val preview: String,
        val lastAt: Long,
        val status: DeliveryStatus,
        val priority: Priority,
        val unread: Int = 0,
    )

    val threads: List<Thread> = listOf(
        Thread(
            peer = peers[0],
            preview = "Reached the shelter, two more with me.",
            lastAt = now() - 2 * 60_000,
            status = DeliveryStatus.DELIVERED,
            priority = Priority.NORMAL,
            unread = 2,
        ),
        Thread(
            peer = peers[1],
            preview = "Water at the north gate. Bring the kit.",
            lastAt = now() - 26 * 60_000,
            status = DeliveryStatus.RELAYED,
            priority = Priority.HIGH,
        ),
        Thread(
            peer = peers[3],
            preview = "Can you hear us? Nobody has signal here.",
            lastAt = now() - 74 * 60_000,
            status = DeliveryStatus.SENT,
            priority = Priority.NORMAL,
        ),
    )

    /** Incoming broadcast, shown in the Emergency history. */
    val alerts: List<LynqMessage> = listOf(
        LynqMessage(
            messageId = "a1",
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = "c14b76f3",
            senderName = "Kabir",
            body = "Trapped on the second floor, stairwell is blocked.",
            createdAt = now() - 9 * 60_000,
            routeHistory = listOf("Kabir", "Meera", "You"),
            status = DeliveryStatus.DELIVERED,
        ),
        LynqMessage(
            messageId = "a2",
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = "8ae0d915",
            senderName = "Priya",
            body = "Medical assistance required. Asthma, no inhaler.",
            createdAt = now() - 52 * 60_000,
            routeHistory = listOf("Priya", "Kabir", "Aarav", "You"),
            status = DeliveryStatus.DELIVERED,
        ),
    )

    /** Short activity strip on the dashboard. */
    val activity: List<LynqMessage> = listOf(
        LynqMessage(
            messageId = "m1",
            type = MessageType.CHAT,
            priority = Priority.NORMAL,
            senderId = "2b7f9c41",
            senderName = "Aarav",
            recipientId = MY_ID,
            body = "Reached the shelter, two more with me.",
            createdAt = now() - 2 * 60_000,
            routeHistory = listOf("Aarav", "You"),
            status = DeliveryStatus.DELIVERED,
        ),
        LynqMessage(
            messageId = "m2",
            type = MessageType.CHAT,
            priority = Priority.HIGH,
            senderId = "5d80e2aa",
            senderName = "Meera",
            recipientId = MY_ID,
            body = "Water at the north gate. Bring the kit.",
            createdAt = now() - 26 * 60_000,
            routeHistory = listOf("Meera", "Kabir", "You"),
            status = DeliveryStatus.RELAYED,
        ),
        LynqMessage(
            messageId = "m3",
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = MY_ID,
            senderName = "You",
            body = "Fire on the third floor, evacuating east.",
            createdAt = now() - 41 * 60_000,
            routeHistory = listOf("You", "Aarav", "Kabir", "Priya"),
            status = DeliveryStatus.DELIVERED,
        ),
    )

    /** Builds the message the Emergency screen "sends" while the engine is stubbed. */
    fun outgoingSos(category: String, note: String, shareLocation: Boolean): LynqMessage =
        LynqMessage(
            messageId = UUID.randomUUID().toString(),
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = MY_ID,
            senderName = "You",
            body = if (note.isBlank()) category else "$category — $note",
            createdAt = System.currentTimeMillis(),
            // Relaying is optimistic here; the engine will overwrite this with real hops.
            routeHistory = listOf("You", "Aarav", "Kabir"),
            status = DeliveryStatus.RELAYED,
            recipientId = null,
        )

    private fun now(): Long = System.currentTimeMillis()
}
