# Lynq — Networking & Mesh Protocol

Lynq uses **Google Nearby Connections** operating in **`Strategy.P2P_CLUSTER`** mode to build a multi-hop, peer-to-peer ad-hoc mesh.

---

## 1. Network Topology & Links

- `Strategy.P2P_CLUSTER` enables high-throughput M-to-N communication where any device can simultaneously connect to multiple nearby phones.
- Nearby Connections provides physical point-to-point links between direct neighbours, but does **not** provide routing.
- Lynq implements application-level store-and-forward mesh routing across arbitrary topologies (e.g. `A ↔ B ↔ C ↔ D`).

---

## 2. Wire Protocol (`WireMessage`)

Wire messages are encoded to UTF-8 JSON bytes via `kotlinx.serialization`:

```kotlin
@Serializable
data class WireMessage(
    val version: Int = 1,
    val messageId: String,                    // UUID
    val type: MessageType,                    // CHAT, SOS, ACK, PRESENCE
    val priority: Priority,                   // CRITICAL, HIGH, NORMAL
    val senderId: String,                     // Public key fingerprint
    val senderName: String,                   // Original sender display name
    val recipientId: String? = null,          // null = broadcast (e.g. SOS)
    val createdAt: Long,
    val expiresAt: Long,
    val ttl: Int,                             // Hop limit (decremented each hop)
    val routeHistory: List<String>,           // Device fingerprints traversed
    val routeNames: List<String>,             // Display names traversed
    val payload: String,                      // Plaintext SOS or AES-GCM base64 ciphertext
    val signature: String? = null,            // ECDSA sender signature
    val ackMessageId: String? = null,         // Referenced message ID if ACK
    val senderPublicKey: String? = null,      // Base64 public key for handshake
)
```

---

## 3. Strict Router Algorithm

Upon receiving a packet from a peer:

1. **Decode & Validate**: Deserialise `WireMessage` from JSON bytes; malformed packets are immediately dropped.
2. **Deduplication**: Check if `messageId` exists in SQLite `seen_messages`. If present, discard packet to prevent network storms and routing loops.
3. **Record Seen**: Insert `messageId` into `seen_messages`.
4. **Local Delivery**:
   - If this device is the targeted recipient or if the message is a broadcast (`SOS`):
     - Decrypt payload if applicable.
     - Store message locally in `messages` table.
     - Emit to UI StateFlows and trigger emergency alerts.
     - Send an application-level `ACK` message back along the route.
     - For 1-to-1 CHAT messages, terminate routing here.
     - For broadcast `SOS` messages, continue to Step 5 so the alert propagates to other branches.
5. **TTL & Expiry Check**: If `ttl <= 1` or current time exceeds `expiresAt`, drop forwarding.
6. **Route Decoration & Decrement**:
   - Decrement `ttl` by 1.
   - Append local device ID to `routeHistory` and local display name to `routeNames`.
7. **Flood Forwarding**: Transmit the packet to all connected peers, **excluding** the peer that forwarded it and any peer already in `routeHistory`.
8. **ACK Confirmation**: When the original sender receives the corresponding `ACK`, its delivery status transitions from `PENDING` / `SENT` / `RELAYED` to **`DELIVERED`**, displaying the complete route traversed (e.g. `You → Aarav → Kabir → Priya`).

---

## 4. Priority Queue

Messages are ordered by priority weight:
- **`CRITICAL` (Weight 0)**: Emergency SOS broadcasts (never blocked).
- **`HIGH` (Weight 1)**: Urgent alerts and medical notes.
- **`NORMAL` (Weight 2)**: Standard chat and messages.

Pending messages without an active route are retained in SQLite `pending_queue` and retried automatically as soon as new peers connect.
