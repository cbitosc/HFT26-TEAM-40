package com.sunless.lynq.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sunless.lynq.LynqApp
import com.sunless.lynq.MainActivity
import com.sunless.lynq.crypto.CryptoManager
import com.sunless.lynq.data.storage.LynqDatabase
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.LynqMessage
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.domain.model.Priority
import com.sunless.lynq.domain.model.WireMessage
import com.sunless.lynq.network.NearbyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The core routing engine for Lynq.
 * Implements the store-and-forward mesh routing algorithm defined in AGENTS.md.
 *
 * - Deduplication via persistent seen-set
 * - TTL hop decrementing and expiration
 * - Multi-hop broadcast flooding (SOS) and unicast routing
 * - Application-level ACK return path
 * - Priority-ordered outgoing queue (CRITICAL > HIGH > NORMAL)
 */
class MessageRouter(
    private val database: LynqDatabase,
    private val cryptoManager: CryptoManager,
    private val nearbyManager: NearbyManager,
    initialMyName: String = "Sunless",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    var myName: String = initialMyName
        private set

    fun updateMyName(newName: String) {
        val clean = newName.trim()
        if (clean.isNotBlank() && clean != myName) {
            myName = clean
            nearbyManager.updateLocalDeviceName(clean)
            scope.launch {
                broadcastPresence()
            }
        }
    }

    private val tag = "LynqMessageRouter"
    val myDeviceId: String get() = cryptoManager.deviceId

    // Live alert events for immediate in-app banner or notifications
    private val _incomingAlerts = MutableSharedFlow<LynqMessage>(extraBufferCapacity = 16)
    val incomingAlerts: SharedFlow<LynqMessage> = _incomingAlerts.asSharedFlow()

    private val greetedPeerIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    init {
        // Listen to incoming raw bytes from NearbyManager
        scope.launch {
            nearbyManager.rawPayloadFlow.collect { (endpointId, bytes) ->
                handleIncomingBytes(endpointId, bytes)
            }
        }

        // Listen for new connections to send PRESENCE and retry pending messages
        scope.launch {
            nearbyManager.connectedPeersFlow.collect { peers ->
                val currentPeerIds = peers.map { it.id }.toSet()

                // Immediately sync active connections into DB
                for (peer in peers) {
                    database.upsertPeer(peer.copy(connected = true, hops = 1, lastSeen = System.currentTimeMillis()))
                }

                // Mark any previously direct peers as disconnected if no longer in connectedPeers
                val allPeers = database.peersFlow.value
                for (p in allPeers) {
                    if (p.connected && !currentPeerIds.contains(p.id)) {
                        database.setPeerConnectionState(p.id, false)
                    }
                }

                greetedPeerIds.retainAll(currentPeerIds)

                val newPeers = peers.filter { greetedPeerIds.add(it.id) }
                if (newPeers.isNotEmpty()) {
                    Log.i(tag, "New peers connected: ${newPeers.map { it.name }}, broadcasting presence")
                    broadcastPresence()
                    retryPendingQueue()
                }
            }
        }
    }

    /**
     * Start the routing engine and peer discovery
     */
    fun start() {
        Log.i(tag, "Starting Lynq MessageRouter...")
        nearbyManager.start()
        scope.launch {
            broadcastPresence()
            retryPendingQueue()
        }
    }

    fun stop() {
        Log.i(tag, "Stopping Lynq MessageRouter...")
        nearbyManager.stop()
    }

    /* -------------------------------------------------------------
     * Outgoing Transmissions
     * ------------------------------------------------------------- */

    /**
     * Send an emergency SOS broadcast across the mesh.
     * SOS is ALWAYS CRITICAL priority, unencrypted for universal readability, and signed.
     */
    suspend fun sendSos(
        category: String,
        details: String = "",
        coordinates: String? = null,
    ): LynqMessage {
        val messageId = UUID.randomUUID().toString()
        val locStr = coordinates ?: "Location not shared"
        val bodyText = if (details.isBlank()) {
            "[$category] SOS alert triggered at $locStr"
        } else {
            "[$category] $details (Location: $locStr)"
        }

        val wire = WireMessage(
            messageId = messageId,
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = myDeviceId,
            senderName = myName,
            recipientId = null, // Broadcast to all
            createdAt = System.currentTimeMillis(),
            ttl = LynqMessage.DEFAULT_HOPS,
            routeHistory = listOf(myDeviceId),
            routeNames = listOf(myName),
            payload = bodyText,
            signature = cryptoManager.sign(bodyText),
            senderPublicKey = cryptoManager.publicKeyString,
        )

        val localMsg = LynqMessage(
            messageId = messageId,
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = myDeviceId,
            senderName = "You",
            recipientId = null,
            body = bodyText,
            createdAt = wire.createdAt,
            ttl = wire.ttl,
            routeHistory = listOf("You"),
            status = DeliveryStatus.PENDING,
        )

        // 1. Mark as seen locally to prevent loops
        database.markMessageSeen(messageId)

        // 2. Save locally in DB
        database.saveMessage(localMsg)

        // 3. Queue and attempt forward
        dispatchWire(wire, localMsg)
        return localMsg
    }

    /**
     * Send a 1-to-1 CHAT message to a specific peer.
     * End-to-end encrypted with the recipient's public key.
     */
    suspend fun sendChatMessage(
        recipient: Peer,
        plainText: String,
        priority: Priority = Priority.NORMAL,
    ): LynqMessage {
        val messageId = UUID.randomUUID().toString()
        val cipherText = cryptoManager.encrypt(plainText, recipient.publicKey)

        val wire = WireMessage(
            messageId = messageId,
            type = MessageType.CHAT,
            priority = priority,
            senderId = myDeviceId,
            senderName = myName,
            recipientId = recipient.id,
            createdAt = System.currentTimeMillis(),
            ttl = LynqMessage.DEFAULT_HOPS,
            routeHistory = listOf(myDeviceId),
            routeNames = listOf(myName),
            payload = cipherText,
            signature = cryptoManager.sign(cipherText),
            senderPublicKey = cryptoManager.publicKeyString,
        )

        val localMsg = LynqMessage(
            messageId = messageId,
            type = MessageType.CHAT,
            priority = priority,
            senderId = myDeviceId,
            senderName = "You",
            recipientId = recipient.id,
            body = plainText,
            createdAt = wire.createdAt,
            ttl = wire.ttl,
            routeHistory = listOf("You"),
            status = DeliveryStatus.PENDING,
        )

        database.markMessageSeen(messageId)
        database.saveMessage(localMsg)

        dispatchWire(wire, localMsg)
        return localMsg
    }

    private suspend fun dispatchWire(wire: WireMessage, localMsg: LynqMessage) {
        val rawBytes = wire.toBytes()
        val directEndpoint = wire.recipientId?.let { nearbyManager.getEndpointIdForPeer(it) }

        val sentCount = if (directEndpoint != null) {
            if (nearbyManager.sendBytes(directEndpoint, rawBytes)) 1 else 0
        } else {
            nearbyManager.flood(rawBytes)
        }

        if (sentCount > 0) {
            val newStatus = if (wire.isBroadcast) DeliveryStatus.RELAYED else DeliveryStatus.SENT
            database.updateDeliveryStatus(wire.messageId, newStatus)
            Log.d(tag, "Message ${wire.messageId} forwarded to $sentCount peer(s), marked $newStatus")
        } else {
            Log.d(tag, "No direct peer available for ${wire.messageId}, enqueuing in store-and-forward queue")
            database.enqueueWire(wire)
        }
    }

    /* -------------------------------------------------------------
     * Incoming Protocol Handling (Strict router algorithm per AGENTS.md)
     * ------------------------------------------------------------- */

    suspend fun handleIncomingBytes(fromEndpointId: String?, rawBytes: ByteArray) {
        // Step 1: Decode and validate structure. Drop malformed messages.
        val wire = WireMessage.fromBytes(rawBytes)
        if (wire == null) {
            Log.w(tag, "Dropped malformed wire payload of size ${rawBytes.size}")
            return
        }

        // Handle PRESENCE handshake
        if (wire.type == MessageType.PRESENCE) {
            handlePresence(fromEndpointId, wire)
            return
        }

        // Step 2: Dedupe - if messageId is in seen-set, drop it.
        if (database.isMessageSeen(wire.messageId)) {
            Log.d(tag, "Dedupe: dropped already seen message ${wire.messageId}")
            return
        }

        // Step 3: Add messageId to seen-set.
        database.markMessageSeen(wire.messageId)

        // Step 4: If this device is the recipient (or broadcast/SOS):
        // decrypt if applicable, store, notify UI, and send ACK back.
        val isDirectPeerChat = wire.type == MessageType.CHAT &&
            nearbyManager.isEndpointConnected(fromEndpointId) &&
            nearbyManager.connectedEndpointCount == 1
        val isRecipient = (wire.recipientId != null && wire.recipientId.replace(" ", "").equals(myDeviceId.replace(" ", ""), ignoreCase = true)) || isDirectPeerChat
        val isSosOrBroadcast = wire.isBroadcast

        if (wire.type == MessageType.ACK) {
            handleAck(wire)
            return
        }

        if (isRecipient || isSosOrBroadcast) {
            val decryptedBody = if (wire.type == MessageType.SOS) {
                wire.payload
            } else {
                cryptoManager.decrypt(wire.payload, wire.senderPublicKey)
            }

            val routeNamesWithMe = wire.routeNames + listOf(myName)
            val incomingMsg = LynqMessage(
                messageId = wire.messageId,
                type = wire.type,
                priority = wire.priority,
                senderId = wire.senderId,
                senderName = wire.senderName,
                recipientId = if (isRecipient) myDeviceId else wire.recipientId,
                body = decryptedBody,
                createdAt = wire.createdAt,
                expiresAt = wire.expiresAt,
                ttl = wire.ttl,
                routeHistory = routeNamesWithMe,
                status = DeliveryStatus.DELIVERED,
            )

            database.saveMessage(incomingMsg)
            Log.i(tag, "Delivered message ${wire.messageId} from ${wire.senderName} (${wire.type})")

            if (wire.type == MessageType.SOS) {
                _incomingAlerts.emit(incomingMsg)
                notifyIncomingSos(incomingMsg)
            }

            // Send application-level ACK back to original sender
            sendAckBack(wire)

            // For 1-to-1 messages where we are the final recipient, stop here.
            if (!isSosOrBroadcast) {
                return
            }
        }

        // Step 5: If ttl <= 0 or expiresAt passed: drop.
        val now = System.currentTimeMillis()
        if (wire.ttl <= 1 || now > wire.expiresAt) {
            Log.d(tag, "TTL expired or TTL <= 1 (ttl=${wire.ttl}) for ${wire.messageId}, dropping forward")
            return
        }

        // Step 6: Persist to the store-and-forward queue
        val nextRouteHistory = wire.routeHistory + listOf(myDeviceId)
        val nextRouteNames = wire.routeNames + listOf(myName)
        val forwardedWire = wire.copy(
            ttl = wire.ttl - 1,
            routeHistory = nextRouteHistory,
            routeNames = nextRouteNames,
        )

        // Step 7 & 8: Forward to all eligible connected peers, excluding the peer
        // it came from and any peer already in routeHistory.
        val rawForwardBytes = forwardedWire.toBytes()
        val count = nearbyManager.flood(rawForwardBytes, excludeEndpointId = fromEndpointId)
        Log.i(tag, "Relayed ${wire.type} ${wire.messageId} to $count peers (TTL remaining: ${forwardedWire.ttl})")
    }

    private suspend fun handlePresence(endpointId: String?, wire: WireMessage) {
        database.reconcilePeer(wire.senderName, wire.senderId, wire.senderPublicKey)
        Log.i(tag, "Reconciled peer presence in DB for ${wire.senderName} (${wire.senderId}) on endpoint $endpointId")

        if (endpointId != null) {
            nearbyManager.updateEndpointIdentity(endpointId, wire.senderId, wire.senderName)
        }

        // Mutual presence reply so both devices learn each other's identity and keys
        if (wire.payload != "PRESENCE_ACK" && endpointId != null) {
            val replyPresence = WireMessage(
                messageId = UUID.randomUUID().toString(),
                type = MessageType.PRESENCE,
                priority = Priority.NORMAL,
                senderId = myDeviceId,
                senderName = myName,
                payload = "PRESENCE_ACK",
                senderPublicKey = cryptoManager.publicKeyString,
                ttl = 1,
            )
            nearbyManager.sendBytes(endpointId, replyPresence.toBytes())
        }
    }

    private fun notifyIncomingSos(msg: LynqMessage) {
        try {
            val context = LynqApp.instance
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "lynq_emergency_alerts",
                    "Emergency SOS Broadcasts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Critical offline emergency alerts from nearby devices"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                }
                nm.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context,
                msg.messageId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notification = NotificationCompat.Builder(context, "lynq_emergency_alerts")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("🚨 EMERGENCY SOS BROADCAST")
                .setContentText("From ${msg.senderName}: ${msg.body}")
                .setStyle(NotificationCompat.BigTextStyle().bigText("🚨 EMERGENCY SOS BROADCAST\nFrom: ${msg.senderName}\n\n${msg.body}"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()

            nm.notify(msg.messageId.hashCode(), notification)
        } catch (e: Exception) {
            Log.e(tag, "Failed to post emergency notification", e)
        }
    }

    private suspend fun sendAckBack(originalWire: WireMessage) {
        val ackId = UUID.randomUUID().toString()
        val ackWire = WireMessage(
            messageId = ackId,
            type = MessageType.ACK,
            priority = Priority.CRITICAL,
            senderId = myDeviceId,
            senderName = myName,
            recipientId = originalWire.senderId,
            createdAt = System.currentTimeMillis(),
            ttl = LynqMessage.DEFAULT_HOPS,
            routeHistory = listOf(myDeviceId),
            routeNames = listOf(myName),
            payload = "ACK",
            ackMessageId = originalWire.messageId,
        )

        Log.d(tag, "Sending ACK for message ${originalWire.messageId} back to ${originalWire.senderName}")
        nearbyManager.flood(ackWire.toBytes())
    }

    private suspend fun handleAck(ackWire: WireMessage) {
        val ackTarget = ackWire.ackMessageId ?: return
        if (ackWire.recipientId == myDeviceId) {
            // We were the original sender! Mark DELIVERED and update route history with the full path!
            val fullRoute = ackWire.routeNames + listOf(myName)
            database.updateDeliveryStatus(ackTarget, DeliveryStatus.DELIVERED, fullRoute)
            database.dequeueWire(ackTarget)
            Log.i(tag, "ACK confirmed! Message $ackTarget DELIVERED. Route: $fullRoute")
        } else {
            // Intermediate relay: forward the ACK onwards
            if (ackWire.ttl > 1) {
                val nextAck = ackWire.copy(
                    ttl = ackWire.ttl - 1,
                    routeHistory = ackWire.routeHistory + listOf(myDeviceId),
                    routeNames = ackWire.routeNames + listOf(myName),
                )
                nearbyManager.flood(nextAck.toBytes())
            }
        }
    }

    private suspend fun broadcastPresence() {
        val presence = WireMessage(
            messageId = UUID.randomUUID().toString(),
            type = MessageType.PRESENCE,
            priority = Priority.NORMAL,
            senderId = myDeviceId,
            senderName = myName,
            payload = "PRESENCE",
            senderPublicKey = cryptoManager.publicKeyString,
            ttl = 1,
        )
        nearbyManager.flood(presence.toBytes())
    }

    private suspend fun retryPendingQueue() {
        val pending = database.getPendingQueue()
        if (pending.isEmpty()) return

        Log.d(tag, "Retrying ${pending.size} pending message(s) from priority queue...")
        for (wire in pending) {
            val count = nearbyManager.flood(wire.toBytes())
            if (count > 0) {
                database.dequeueWire(wire.messageId)
                database.updateDeliveryStatus(wire.messageId, DeliveryStatus.SENT)
            }
        }
    }

    /**
     * Demo simulation harness: Allows testing multi-hop relaying (A -> B -> C -> D)
     * and ACK loopback on a single device or emulator for visual verification.
     */
    suspend fun simulateMultiHopDemo(
        originName: String = "You",
        relayNodes: List<String> = listOf("Aarav", "Kabir", "Priya"),
        category: String = "Medical",
        notes: String = "Asthma emergency, need inhaler",
    ) {
        val msgId = UUID.randomUUID().toString()
        val path = mutableListOf(originName)
        val bodyText = "[$category] $notes (Location: 28.6139° N, 77.2090° E)"

        // Step 1: Origin queues SOS
        val originMsg = LynqMessage(
            messageId = msgId,
            type = MessageType.SOS,
            priority = Priority.CRITICAL,
            senderId = myDeviceId,
            senderName = originName,
            recipientId = null,
            body = bodyText,
            createdAt = System.currentTimeMillis(),
            routeHistory = listOf(originName),
            status = DeliveryStatus.PENDING,
        )
        database.saveMessage(originMsg)

        // Step 2: Relaying through each hop
        for (relay in relayNodes) {
            kotlinx.coroutines.delay(600)
            path.add(relay)
            database.updateDeliveryStatus(msgId, DeliveryStatus.RELAYED, path.toList())
        }

        // Step 3: Recipient reached and ACK returned
        kotlinx.coroutines.delay(700)
        database.updateDeliveryStatus(msgId, DeliveryStatus.DELIVERED, path.toList())
    }
}
