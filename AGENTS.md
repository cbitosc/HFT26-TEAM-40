# AGENTS.md — Lynq

> Brief for the AI coding agent working on **Lynq**.
> Read this file fully before writing any code. Re-read the relevant section before starting each task.

---

## 0. TL;DR

Lynq is an **offline-first emergency communication app for Android**. When cellular and internet are down, nearby phones running Lynq form a local peer-to-peer network and **relay messages hop by hop** until they reach the recipient. The headline feature is an **SOS broadcast that travels across multiple devices (A → B → C → D)** even when A and D cannot talk directly.

This is a **24-hour hackathon project**. Priorities:

1. A working multi-device, multi-hop demo on real phones.
2. Reliability of the networking engine over visual polish.
3. A clean, distinctive UI that is *not* a WhatsApp clone.
4. Everything else (AI, advanced battery tuning) is a bonus and must never block the core.

> The app was previously called **FLUX** in early planning docs. The name is now **Lynq**. Use "Lynq" in all code, strings, package names and docs.

---

## 1. Reference repository (port features from here)

The team has an existing, similar Android app. **Prefer porting proven code over writing from scratch.**

- **Reference repo:** `<PASTE_GITHUB_REPO_URL_HERE>`
- **Local path (if cloned):** `<PASTE_LOCAL_PATH_HERE>`

### How to use the reference repo

1. **Explore first.** Read its README, module layout, `build.gradle(.kts)` files, and dependency versions. Summarise what is reusable before copying anything.
2. **Look for reusable pieces**, in this order:
   - Compose theme, design system, reusable components, navigation setup
   - Nearby Connections / P2P wrappers, permission handling
   - Room entities/DAOs, repositories, ViewModel patterns
   - Encryption / key handling utilities
   - Any foreground service or background work setup
3. **Port, don't paste blindly.** When moving code:
   - Rename packages/classes to Lynq conventions.
   - Remove dead code, unrelated features, hardcoded keys and secrets.
   - Adapt to the Lynq message protocol in section 5 (do not bend Lynq's protocol to fit old code).
   - Keep it compiling after each ported piece.
4. **Log what you ported** in `docs/PORTING_LOG.md`: source file → destination file → what changed.
5. **Never copy** API keys, keystores, `google-services.json`, `local.properties`, or any user data from the reference repo.
6. If the reference code is poor, buggy, or conflicts with the architecture below, say so and rewrite it. Architecture here wins.

---

## 2. Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3, custom theme) |
| P2P | Google Nearby Connections, **`Strategy.P2P_CLUSTER`** |
| Local storage | Room |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt (or manual DI if the reference repo already uses something else — match it) |
| Crypto | Android Keystore + AES-256-GCM, public/private keypair for identity |
| Background | Foreground service only where needed; WorkManager for retry/housekeeping |
| Location | Fused Location Provider, **optional and user-authorised** |
| Optional AI | Python + FastAPI backend calling Featherless (OpenAI-compatible API) |

Match dependency versions to the reference repo where possible to avoid Gradle conflicts. Use a version catalog (`libs.versions.toml`).

---

## 3. Architecture

The **messaging engine must be independent of the UI.** It should be testable without Compose.

```
┌──────────────────────── UI (Jetpack Compose) ────────────────────────┐
│ Dashboard │ Messages │ Emergency │ Mesh Explorer │ Settings/Identity │
└───────────────────────────────┬───────────────────────────────────────┘
                                │ ViewModels (StateFlow)
┌───────────────────────────────▼───────────────────────────────────────┐
│                          Messaging Engine                              │
│  MessageRouter · PriorityQueue · DeliveryStatusManager · CryptoManager │
└───────────┬───────────────────────────────────────────┬───────────────┘
            │                                           │
┌───────────▼──────────────┐                 ┌──────────▼──────────────┐
│ NearbyManager             │                 │ Room Database            │
│ discovery/connect/payload │                 │ messages, peers, queue   │
└───────────────────────────┘                 └─────────────────────────┘

(Optional, internet only, fully decoupled)  AI Service → FastAPI → Featherless
```

### Suggested package layout

```
com.<team>.lynq/
├── ui/            // screens, components, theme, navigation
├── engine/        // MessageRouter, PriorityQueue, DeliveryStatusManager
├── network/       // NearbyManager, ConnectionState, PayloadCodec
├── crypto/        // CryptoManager, KeyStore wrapper, identity
├── data/          // Room entities, DAOs, repositories
├── domain/        // models: Message, Peer, Priority, DeliveryStatus
├── location/      // optional location provider
├── service/       // foreground service
├── ai/            // optional AI client (isolated)
└── util/
```

### Module responsibilities

| Module | Responsibility |
|---|---|
| NearbyManager | Advertise, discover, connect, send/receive payloads, expose peer list as Flow |
| MessageRouter | Dedupe, TTL check, deliver locally or forward, trigger acks |
| PriorityQueue | Order outgoing messages: CRITICAL > HIGH > NORMAL |
| CryptoManager | Encrypt/decrypt/sign/verify, key storage |
| DeliveryStatusManager | Track and expose per-message status |
| Room DB | Persist messages, pending forwards, peers, seen-message IDs |
| AI service | Summarise/translate/classify **only when online**, never in the critical path |

---

## 4. Networking rules (the heart of the project)

- Use **`P2P_CLUSTER`** so each device can hold multiple connections.
- Nearby Connections gives **links, not routing**. Lynq must implement multi-hop forwarding itself.
- A successful payload transfer only proves the **next hop** received it. Final delivery requires an **application-level ACK** that travels back.
- Reuse connections. Do **not** restart discovery/advertising in a tight loop.
- Handle disconnects and reconnects gracefully; re-queue unsent messages.

### Router algorithm (implement exactly this)

On receiving a message from a connected peer:

1. **Decode** and validate structure. Drop malformed messages.
2. **Dedupe:** if `messageId` is in the seen-set (persisted), drop it.
3. Add `messageId` to seen-set.
4. **If this device is the recipient** (or the message is a broadcast/SOS): decrypt if applicable, store, notify UI, and send an ACK back along the route.
5. **If `ttl <= 0` or `expiresAt` passed:** drop.
6. Persist to the store-and-forward queue.
7. **Decrement TTL**, append this device ID to `routeHistory`.
8. Forward to all eligible connected peers, **excluding** the peer it came from and any peer already in `routeHistory`.
9. Update delivery status when ACKs arrive.

For SOS/broadcast messages: step 4 still stores and displays the message locally **and** continues to step 5+ so it keeps propagating.

### Store-and-forward

- If no peers are connected, keep the message as `PENDING` and retry when a peer connects.
- Bound the queue size and expire old messages.

---

## 5. Message protocol

Keep the wire format small, versioned and explicit. Suggested model:

```kotlin
data class LynqMessage(
    val version: Int = 1,
    val messageId: String,        // UUID
    val type: MessageType,        // CHAT, SOS, ACK, PRESENCE
    val priority: Priority,       // CRITICAL, HIGH, NORMAL
    val senderId: String,         // device identity (public key fingerprint)
    val recipientId: String?,     // null = broadcast (e.g. SOS)
    val createdAt: Long,
    val expiresAt: Long,
    val ttl: Int,                 // hop limit
    val routeHistory: List<String>,
    val payload: ByteArray,       // encrypted body for CHAT; SOS body per section 6
    val signature: ByteArray?     // sender signature
)
```

Serialisation: Kotlinx Serialization (JSON for speed of development; switch to Protobuf/CBOR only if time allows).

### Priority

| Priority | Used for |
|---|---|
| CRITICAL | SOS alerts |
| HIGH | Emergency messages |
| NORMAL | Regular chat |

- Outgoing and forwarding queues are processed **highest priority first**.
- **A user-triggered SOS is always CRITICAL.** Never depend on AI classification to set SOS priority.

### Delivery status lifecycle

`PENDING` → `SENT` → `RELAYED` → `DELIVERED` (and `FAILED`/`EXPIRED` for terminal errors)

| Status | Meaning |
|---|---|
| PENDING | Saved locally, no connection yet |
| SENT | Handed to the next device |
| RELAYED | Confirmed forwarded by an intermediate device |
| DELIVERED | Recipient's ACK received |

---

## 6. SOS system (the centrepiece)

Dedicated **Emergency Centre** screen with:

- Large, obvious SOS button (with hold-to-confirm or confirm dialog to avoid accidental sends)
- Emergency category: Medical, Fire, Rescue/Trapped, Other
- Optional free-text message
- **Optional** location sharing (explicit permission; SOS must still work with no location)
- Nearby device / relay count
- Live delivery and relay status
- History of sent and received alerts

Example SOS body:

```
SOS ALERT
User: <display name / device id>
Emergency: Medical assistance required
Location: <lat,lng or "not shared">
Time: 18:42
Priority: CRITICAL
Status: RELAYING
```

Rules:
- SOS must **never** be blocked by AI, network lookups, location failure, or UI state.
- Received SOS triggers a prominent in-app alert (and notification when allowed).
- SOS relays through **every** reachable device, not just a single path.

---

## 7. Security

| Feature | Approach |
|---|---|
| Message encryption | AES-256-GCM |
| Key storage | Android Keystore |
| Device identity | Public/private keypair; ID = fingerprint of public key |
| Authenticity | Sender signature, verified by recipient |
| Integrity | GCM authentication tag |
| Replay/duplicate protection | Unique message IDs + persisted seen-set + expiry |

Important:
- Per-link encryption from Nearby is **not** end-to-end. The **original sender** encrypts for the **final recipient**; relays only see ciphertext and routing metadata.
- SOS broadcasts are intended for all nearby devices, so their body may be signed but readable. Keep this distinction explicit in code comments and docs.
- Exchange public keys during connection/handshake (or via a PRESENCE message).
- Never log plaintext messages or key material.
- No secrets, keystores or API keys in the repo.

---

## 8. UI / UX direction

Lynq should feel like **an emergency communication tool with a live network view**, not a generic chat app.

- Landing screen: **Network Dashboard** (not a chat list).
- Design direction: **<CHOOSE: premium dark & futuristic / minimal & clean / professional emergency command center>** — ask the team if unspecified; default to premium dark.
- Use a single custom theme (colors, typography, shapes) defined once and reused.
- High contrast, large touch targets for critical actions, readable under stress.

### Screens

1. **Network Dashboard** — nearby device count, connection status, active relays, pending messages, quick SOS
2. **Messages** — conversations, delivery indicators, priority labels, history
3. **Emergency Centre** — SOS, categories, location toggle, alert tracking
4. **Mesh Explorer** — connected devices, topology graph, message routes, relay activity (the "wow" screen for judges)
5. **Settings & Identity** — device name/identity, security, battery preferences, permissions

Show delivery status and the **route taken** (e.g. A → B → C → D) wherever possible.

---

## 9. Permissions and platform notes

- Nearby Connections requires Google Play services; test on the exact demo phones.
- Handle runtime permissions by API level: Bluetooth (scan/advertise/connect), Nearby Wi-Fi Devices, fine/coarse location (needed on older APIs for discovery), notifications.
- Explain each permission in plain language before requesting.
- Don't promise uninterrupted background relaying when the app is closed. Test foreground and background separately.
- Use a foreground service only when genuinely needed and allowed.

---

## 10. Optional AI assistant (LOW PRIORITY)

Only after the core mesh works.

- Features: summarise, translate, classify (medical/fire/rescue/general), suggest priority for *human triage*.
- Python + FastAPI backend, Featherless API (OpenAI-compatible). **API keys live on the backend only, never in the APK.**
- Works only when internet is available.
- **Fully decoupled:** the app must never wait on AI to send, forward or display anything.
- If time is short, skip this entirely.

---

## 11. Build plan (24 hours)

| Hours | Focus |
|---|---|
| 0–2 | Project setup, Compose navigation, theme, repo (port from reference repo here) |
| 2–6 | Nearby Connections: discovery, connect, direct messaging |
| 6–11 | Protocol, Room, queues, SOS priority, ACKs |
| 11–15 | Forwarding, TTL, dedupe, multi-device testing, topology view |
| 15–18 | Encryption, authentication, location, SOS broadcast |
| 18–21 | Polish, optional AI |
| 21–24 | Physical device testing, bug fixes, README, architecture docs, demo video |

If behind schedule, cut in this order: AI → advanced battery work → extra screens/animations. **Never cut multi-hop delivery or SOS.**

---

## 12. Demo target (everything should serve this)

Four Android devices, no internet.

1. Launch Lynq on all four; devices discover and connect.
2. Arrange so A↔B, B↔C, C↔D connect, but A and D cannot reach each other directly.
3. Send an SOS from A.
4. Message visibly travels through B and C.
5. D shows the SOS.
6. ACK returns; A shows **DELIVERED** and the route taken.

Success criteria: direct offline messaging, multi-hop delivery, SOS prioritisation, encrypted messages, delivery acknowledgements, live network visualisation.

> Do **not** claim Lynq can contact emergency services without a cellular or internet path. It relays alerts among participating nearby devices only.

---

## 13. Working agreement for the agent

**Do**
- Work in small, compiling increments. Build and run after each major step.
- Commit often with clear messages (`feat(router): add TTL + dedupe`).
- Write unit tests for the router, queue ordering, dedupe and crypto round-trips.
- Keep engine code free of Android UI dependencies where possible.
- Add KDoc to public engine/network APIs.
- Keep a running `docs/PROGRESS.md` (done / in progress / blocked).
- Ask the team when something is genuinely ambiguous (design direction, team size, device count, package name). Otherwise make a sensible choice and document it.

**Don't**
- Don't build a production-grade mesh protocol. Simple flood-with-TTL + dedupe is enough.
- Don't add features outside this document without asking.
- Don't block SOS on anything.
- Don't commit secrets, keystores, `local.properties`, or user data.
- Don't rewrite working reference-repo code just for style.
- Don't leave failing builds on the main branch.

### Definition of done (per feature)
- Compiles and runs on a physical device
- Handles disconnect / permission-denied / empty states
- No crashes on rotation or backgrounding
- Relevant unit tests pass
- Mentioned in `docs/PROGRESS.md`

---

## 14. Repository and submission checklist

```
lynq/
├── app/src/main/
├── docs/
│   ├── architecture.md
│   ├── networking.md
│   ├── PORTING_LOG.md
│   ├── PROGRESS.md
│   └── screenshots/
├── README.md
├── LICENSE
└── .gitignore
```

README must cover: problem and motivation, how Lynq works, stack, architecture and message flow, install steps, permissions, offline limitations, screenshots and demo video.

---

## 15. First actions for the agent

1. Read this file.
2. Clone/explore the reference repo and write a short reuse summary (what to port, what to skip).
3. Confirm package name, min/target SDK, and design direction with the team.
4. Set up the project skeleton, theme and navigation (port from reference where possible).
5. Build `NearbyManager` and prove two devices can exchange a message.
6. Build the router (dedupe + TTL + forward + ACK) and prove 3+ devices relay a message.
7. Add SOS, encryption, then visualisation.
