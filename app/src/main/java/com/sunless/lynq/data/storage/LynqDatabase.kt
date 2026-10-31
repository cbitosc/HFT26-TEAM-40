package com.sunless.lynq.data.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.LynqMessage
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Peer
import com.sunless.lynq.domain.model.Priority
import com.sunless.lynq.domain.model.WireMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Robust, lightweight SQLite storage for Lynq mesh networking.
 * Persists messages, peers, seen message IDs (deduplication), and outgoing queue.
 */
class LynqDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val json = Json { ignoreUnknownKeys = true }

    // Live reactive state flows for the UI
    private val _messagesFlow = MutableStateFlow<List<LynqMessage>>(emptyList())
    val messagesFlow: StateFlow<List<LynqMessage>> = _messagesFlow.asStateFlow()

    private val _peersFlow = MutableStateFlow<List<Peer>>(emptyList())
    val peersFlow: StateFlow<List<Peer>> = _peersFlow.asStateFlow()

    init {
        // Initial load of messages and peers into memory
        refreshCache()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS messages (
                message_id TEXT PRIMARY KEY,
                type TEXT NOT NULL,
                priority TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                recipient_id TEXT,
                body TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL,
                ttl INTEGER NOT NULL,
                route_history TEXT NOT NULL,
                status TEXT NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS peers (
                peer_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                connected INTEGER NOT NULL,
                relaying INTEGER NOT NULL,
                hops INTEGER NOT NULL,
                last_seen INTEGER NOT NULL,
                public_key TEXT
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS seen_messages (
                message_id TEXT PRIMARY KEY,
                seen_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_queue (
                message_id TEXT PRIMARY KEY,
                priority_weight INTEGER NOT NULL,
                wire_json TEXT NOT NULL,
                queued_at INTEGER NOT NULL
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL("DROP TABLE IF EXISTS peers")
        db.execSQL("DROP TABLE IF EXISTS seen_messages")
        db.execSQL("DROP TABLE IF EXISTS pending_queue")
        onCreate(db)
    }

    fun refreshCache() {
        try {
            val msgs = queryAllMessages()
            val peers = queryAllPeers()
            _messagesFlow.value = msgs
            _peersFlow.value = peers
        } catch (_: Exception) {
            // Database might still be initializing
        }
    }

    /* -------------------------------------------------------------
     * Message Operations
     * ------------------------------------------------------------- */

    suspend fun saveMessage(msg: LynqMessage) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("message_id", msg.messageId)
            put("type", msg.type.name)
            put("priority", msg.priority.name)
            put("sender_id", msg.senderId)
            put("sender_name", msg.senderName)
            put("recipient_id", msg.recipientId)
            put("body", msg.body)
            put("created_at", msg.createdAt)
            put("expires_at", msg.expiresAt)
            put("ttl", msg.ttl)
            put("route_history", json.encodeToString(msg.routeHistory))
            put("status", msg.status.name)
        }
        writableDatabase.insertWithOnConflict("messages", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        refreshCache()
    }

    suspend fun updateDeliveryStatus(messageId: String, status: DeliveryStatus, routeHistory: List<String>? = null) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("status", status.name)
            if (routeHistory != null) {
                put("route_history", json.encodeToString(routeHistory))
            }
        }
        writableDatabase.update("messages", cv, "message_id = ?", arrayOf(messageId))
        refreshCache()
    }

    suspend fun clearAlertHistory() = withContext(Dispatchers.IO) {
        writableDatabase.delete("messages", "type = ?", arrayOf(MessageType.SOS.name))
        refreshCache()
    }

    fun getMessage(messageId: String): LynqMessage? {
        val cursor = readableDatabase.query(
            "messages",
            null,
            "message_id = ?",
            arrayOf(messageId),
            null,
            null,
            null,
        )
        return cursor.use {
            if (it.moveToFirst()) readMessageRow(it) else null
        }
    }

    private fun queryAllMessages(): List<LynqMessage> {
        val list = mutableListOf<LynqMessage>()
        val cursor = readableDatabase.query(
            "messages",
            null,
            null,
            null,
            null,
            null,
            "created_at DESC",
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(readMessageRow(it))
            }
        }
        return list
    }

    private fun readMessageRow(cursor: android.database.Cursor): LynqMessage {
        val historyJson = cursor.getString(cursor.getColumnIndexOrThrow("route_history"))
        val historyList = runCatching {
            json.decodeFromString<List<String>>(historyJson)
        }.getOrDefault(emptyList())

        return LynqMessage(
            messageId = cursor.getString(cursor.getColumnIndexOrThrow("message_id")),
            type = MessageType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("type"))),
            priority = Priority.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("priority"))),
            senderId = cursor.getString(cursor.getColumnIndexOrThrow("sender_id")),
            senderName = cursor.getString(cursor.getColumnIndexOrThrow("sender_name")),
            recipientId = cursor.getString(cursor.getColumnIndexOrThrow("recipient_id")),
            body = cursor.getString(cursor.getColumnIndexOrThrow("body")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow("expires_at")),
            ttl = cursor.getInt(cursor.getColumnIndexOrThrow("ttl")),
            routeHistory = historyList,
            status = DeliveryStatus.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("status"))),
        )
    }

    /* -------------------------------------------------------------
     * Peer Operations
     * ------------------------------------------------------------- */

    suspend fun upsertPeer(peer: Peer) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("peer_id", peer.id)
            put("name", peer.name)
            put("connected", if (peer.connected) 1 else 0)
            put("relaying", if (peer.relaying) 1 else 0)
            put("hops", peer.hops)
            put("last_seen", peer.lastSeen)
            put("public_key", peer.publicKey)
        }
        writableDatabase.insertWithOnConflict("peers", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        refreshCache()
    }

    suspend fun setPeerConnectionState(peerId: String, connected: Boolean) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("connected", if (connected) 1 else 0)
            put("last_seen", System.currentTimeMillis())
        }
        writableDatabase.update("peers", cv, "peer_id = ?", arrayOf(peerId))
        refreshCache()
    }

    suspend fun reconcilePeer(name: String, newPeerId: String, newPublicKey: String?) = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanId = newPeerId.trim()
        if (cleanName.isBlank() || cleanName.startsWith("Peer-", ignoreCase = true) || cleanId.length <= 4) {
            return@withContext
        }
        // Purge any phantom or unverified records before inserting real cryptographic peer
        writableDatabase.delete("peers", "name = ? AND peer_id != ?", arrayOf(cleanName, cleanId))
        writableDatabase.delete("peers", "name LIKE 'Peer-%' OR name = 'Peer' OR LENGTH(peer_id) <= 4", null)

        val cv = ContentValues().apply {
            put("peer_id", cleanId)
            put("name", cleanName)
            put("connected", 1)
            put("relaying", 0)
            put("hops", 1)
            put("last_seen", System.currentTimeMillis())
            if (newPublicKey != null) {
                put("public_key", newPublicKey)
            }
        }
        writableDatabase.insertWithOnConflict("peers", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        refreshCache()
    }

    suspend fun deletePeer(peerId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("peers", "peer_id = ?", arrayOf(peerId))
        refreshCache()
    }

    suspend fun purgePhantomPeers() = withContext(Dispatchers.IO) {
        writableDatabase.delete(
            "peers",
            "name LIKE 'Peer-%' OR name = 'Peer' OR LENGTH(peer_id) <= 4",
            null,
        )
        refreshCache()
    }

    suspend fun removeDemoPeers() = withContext(Dispatchers.IO) {
        val demoIds = arrayOf("2b7f9c41", "5d80e2aa", "c14b76f3", "8ae0d915")
        val placeholders = demoIds.joinToString(",") { "?" }
        writableDatabase.delete(
            "peers",
            "peer_id IN ($placeholders) OR name IN ('Aarav', 'Meera', 'Kabir', 'Priya') OR name LIKE 'Peer-%' OR name = 'Peer' OR LENGTH(peer_id) <= 4",
            demoIds,
        )
        refreshCache()
    }

    fun getPeer(peerId: String): Peer? {
        val cursor = readableDatabase.query(
            "peers",
            null,
            "peer_id = ?",
            arrayOf(peerId),
            null,
            null,
            null,
        )
        return cursor.use {
            if (it.moveToFirst()) readPeerRow(it) else null
        }
    }

    private fun queryAllPeers(): List<Peer> {
        val list = mutableListOf<Peer>()
        val cursor = readableDatabase.query(
            "peers",
            null,
            null,
            null,
            null,
            null,
            "connected DESC, name COLLATE NOCASE ASC, peer_id ASC",
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(readPeerRow(it))
            }
        }
        return list
    }

    private fun readPeerRow(cursor: android.database.Cursor): Peer {
        return Peer(
            id = cursor.getString(cursor.getColumnIndexOrThrow("peer_id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            connected = cursor.getInt(cursor.getColumnIndexOrThrow("connected")) == 1,
            relaying = cursor.getInt(cursor.getColumnIndexOrThrow("relaying")) == 1,
            hops = cursor.getInt(cursor.getColumnIndexOrThrow("hops")),
            lastSeen = cursor.getLong(cursor.getColumnIndexOrThrow("last_seen")),
            publicKey = cursor.getString(cursor.getColumnIndexOrThrow("public_key")),
        )
    }

    /* -------------------------------------------------------------
     * Deduplication: Seen Message IDs
     * ------------------------------------------------------------- */

    fun isMessageSeen(messageId: String): Boolean {
        val cursor = readableDatabase.query(
            "seen_messages",
            arrayOf("message_id"),
            "message_id = ?",
            arrayOf(messageId),
            null,
            null,
            null,
        )
        return cursor.use { it.moveToFirst() }
    }

    fun markMessageSeen(messageId: String) {
        val cv = ContentValues().apply {
            put("message_id", messageId)
            put("seen_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("seen_messages", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /* -------------------------------------------------------------
     * Store-and-Forward Queue (Priority Ordered: CRITICAL > HIGH > NORMAL)
     * ------------------------------------------------------------- */

    suspend fun enqueueWire(wire: WireMessage) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("message_id", wire.messageId)
            put("priority_weight", wire.priority.weight)
            put("wire_json", json.encodeToString(wire))
            put("queued_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("pending_queue", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun dequeueWire(messageId: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("pending_queue", "message_id = ?", arrayOf(messageId))
    }

    suspend fun getPendingQueue(): List<WireMessage> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WireMessage>()
        val cursor = readableDatabase.query(
            "pending_queue",
            arrayOf("wire_json"),
            null,
            null,
            null,
            null,
            "priority_weight ASC, queued_at ASC",
        )
        cursor.use {
            while (it.moveToNext()) {
                val wireJson = it.getString(0)
                runCatching {
                    json.decodeFromString<WireMessage>(wireJson)
                }.onSuccess { msg -> list.add(msg) }
            }
        }
        list
    }

    companion object {
        private const val DATABASE_NAME = "lynq_mesh.db"
        private const val DATABASE_VERSION = 1
    }
}
