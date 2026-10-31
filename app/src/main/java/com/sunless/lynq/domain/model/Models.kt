package com.sunless.lynq.domain.model

import kotlinx.serialization.Serializable

/**
 * Wire types. [ACK] and [PRESENCE] never reach the UI as normal messages; they are
 * handled by the engine.
 */
@Serializable
enum class MessageType {
    CHAT,
    SOS,
    ACK,
    PRESENCE,
}

/** Processing order for the outgoing queue. Lower [weight] wins. */
@Serializable
enum class Priority(val weight: Int) {
    CRITICAL(0),
    HIGH(1),
    NORMAL(2),
}

/**
 * Lifecycle of an outgoing message.
 *
 * `PENDING` -> `SENT` -> `RELAYED` -> `DELIVERED`.
 */
@Serializable
enum class DeliveryStatus(val label: String) {
    PENDING("Queued"),
    SENT("Sent"),
    RELAYED("Relaying"),
    DELIVERED("Delivered"),
    FAILED("Failed"),
    EXPIRED("Expired"),
}

/** A device we can currently (or recently could) reach. */
@Serializable
data class Peer(
    /** Fingerprint of the peer's public key. Stable identity across sessions. */
    val id: String,
    val name: String,
    val connected: Boolean = true,
    /** True when this peer is currently carrying our traffic for someone else. */
    val relaying: Boolean = false,
    /** Distance in hops from this device; 1 = direct link. */
    val hops: Int = 1,
    val lastSeen: Long = System.currentTimeMillis(),
    /** Optional base64-encoded public key for end-to-end encryption */
    val publicKey: String? = null,
)

/**
 * UI-facing view of a message.
 *
 * This is deliberately *not* the wire format: [body] is already decrypted, and
 * [routeHistory] is a list of display names. The engine owns serialisation and
 * crypto (see docs/architecture.md).
 */
data class LynqMessage(
    val messageId: String,
    val type: MessageType,
    val priority: Priority,
    val senderId: String,
    val senderName: String,
    val recipientId: String? = null,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DEFAULT_TTL_MS,
    val ttl: Int = DEFAULT_HOPS,
    /** Device names this message has passed through, oldest first. */
    val routeHistory: List<String> = emptyList(),
    val status: DeliveryStatus = DeliveryStatus.PENDING,
) {
    val isBroadcast: Boolean get() = recipientId == null

    companion object {
        const val DEFAULT_HOPS = 6
        const val DEFAULT_TTL_MS = 6 * 60 * 60 * 1000L
    }
}
