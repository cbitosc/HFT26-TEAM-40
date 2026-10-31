package com.sunless.lynq

import com.sunless.lynq.crypto.CryptoManager
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Priority
import com.sunless.lynq.domain.model.WireMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MeshProtocolTest {

    @Test
    fun testWireMessageSerializationRoundTrip() {
        val original = WireMessage(
            version = 1,
            messageId = UUID.randomUUID().toString(),
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = "9f3c 41d8 a7e5 4b02",
            senderName = "Sunless",
            recipientId = null,
            createdAt = 1700000000000L,
            expiresAt = 1700021600000L,
            ttl = 6,
            routeHistory = listOf("dev-a", "dev-b"),
            routeNames = listOf("You", "Aarav"),
            payload = "[Medical] Trapped on 2nd floor",
            signature = "MEQCIA...test_signature",
            ackMessageId = null,
        )

        val bytes = original.toBytes()
        assertTrue("Serialized bytes must not be empty", bytes.isNotEmpty())

        val restored = WireMessage.fromBytes(bytes)
        assertNotNull("Deserialized message must not be null", restored)
        assertEquals(original.messageId, restored?.messageId)
        assertEquals(original.type, restored?.type)
        assertEquals(original.priority, restored?.priority)
        assertEquals(original.senderId, restored?.senderId)
        assertEquals(original.senderName, restored?.senderName)
        assertEquals(original.recipientId, restored?.recipientId)
        assertEquals(original.ttl, restored?.ttl)
        assertEquals(original.routeHistory, restored?.routeHistory)
        assertEquals(original.routeNames, restored?.routeNames)
        assertEquals(original.payload, restored?.payload)
        assertEquals(original.signature, restored?.signature)
    }

    @Test
    fun testCryptoManagerKeyGenerationAndSignVerify() {
        val crypto = CryptoManager()

        assertNotNull("Public key string must not be null", crypto.publicKeyString)
        assertNotNull("Device ID must not be null", crypto.deviceId)
        assertEquals("Device ID should have 4 groups of 4 hex chars", 19, crypto.deviceId.length)

        val message = "CRITICAL EMERGENCY: Need medical assistance at 28.6139° N, 77.2090° E"
        val signature = crypto.sign(message)
        assertTrue("Signature should be generated", signature.isNotEmpty())

        // Verification with correct key
        val isValid = crypto.verify(message, signature, crypto.publicKeyString)
        assertTrue("Signature must verify successfully", isValid)

        // Verification with tampered message must fail
        val tampered = crypto.verify(message + " (tampered)", signature, crypto.publicKeyString)
        assertFalse("Tampered message signature verification must fail", tampered)
    }

    @Test
    fun testEndToEndEncryptionRoundTrip() {
        val aliceCrypto = CryptoManager()
        val bobCrypto = CryptoManager()

        val secretMessage = "Meet at the east gate at 22:00."

        // Alice encrypts for Bob
        val cipherText = aliceCrypto.encrypt(secretMessage, bobCrypto.publicKeyString)
        assertTrue("Ciphertext should differ from plaintext", cipherText != secretMessage)

        // Bob decrypts Alice's message using Alice's public key
        val decrypted = bobCrypto.decrypt(cipherText, aliceCrypto.publicKeyString)
        assertEquals("Decrypted message must match original plaintext", secretMessage, decrypted)
    }

    @Test
    fun testPriorityOrdering() {
        val priorities = listOf(Priority.NORMAL, Priority.CRITICAL, Priority.HIGH)
        val sorted = priorities.sortedBy { it.weight }

        assertEquals(Priority.CRITICAL, sorted[0])
        assertEquals(Priority.HIGH, sorted[1])
        assertEquals(Priority.NORMAL, sorted[2])
    }

    @Test
    fun testTtlDecrementAndRouteHistory() {
        val initialWire = WireMessage(
            messageId = "test-msg-1",
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = "dev-a",
            senderName = "Aarav",
            ttl = 6,
            routeHistory = listOf("dev-a"),
            routeNames = listOf("Aarav"),
            payload = "SOS",
        )

        // Relay node B processes and forwards
        val forwardedWire = initialWire.copy(
            ttl = initialWire.ttl - 1,
            routeHistory = initialWire.routeHistory + listOf("dev-b"),
            routeNames = initialWire.routeNames + listOf("Kabir"),
        )

        assertEquals(5, forwardedWire.ttl)
        assertEquals(listOf("dev-a", "dev-b"), forwardedWire.routeHistory)
        assertEquals(listOf("Aarav", "Kabir"), forwardedWire.routeNames)
    }
}
