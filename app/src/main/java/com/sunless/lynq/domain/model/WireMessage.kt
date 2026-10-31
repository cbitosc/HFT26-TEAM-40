package com.sunless.lynq.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * On-the-wire protocol model for Lynq P2P mesh network.
 * Serialized to JSON UTF-8 bytes transmitted over Nearby Connections.
 */
@Serializable
data class WireMessage(
    val version: Int = 1,
    val messageId: String,
    val type: MessageType,
    val priority: Priority,
    val senderId: String,
    val senderName: String,
    val recipientId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + LynqMessage.DEFAULT_TTL_MS,
    val ttl: Int = LynqMessage.DEFAULT_HOPS,
    val routeHistory: List<String> = emptyList(),
    val routeNames: List<String> = emptyList(),
    val payload: String,
    val signature: String? = null,
    val ackMessageId: String? = null,
    val senderPublicKey: String? = null,
) {
    val isBroadcast: Boolean get() = recipientId == null

    fun toBytes(): ByteArray = jsonCodec.encodeToString(serializer(), this).encodeToByteArray()

    companion object {
        private val jsonCodec = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromBytes(bytes: ByteArray): WireMessage? = runCatching {
            jsonCodec.decodeFromString(serializer(), bytes.decodeToString())
        }.getOrNull()
    }
}
