# Lynq — System Architecture

Lynq is an offline-first peer-to-peer emergency communication network for Android. When cellular, satellite, and municipal internet infrastructure fail, nearby phones running Lynq form an ad-hoc local mesh network and relay emergency alerts hop-by-hop.

---

## 1. High-Level Architecture

The messaging engine is strictly decoupled from the UI.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI                              │
│   Dashboard  │  Messages  │  Emergency SOS  │  Mesh Explorer  │  Settings│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ ViewModels (StateFlow)
┌───────────────────────────────────▼────────────────────────────────────┐
│                             Lynq Engine                                │
│       MessageRouter  ·  PriorityQueue  ·  DeliveryStatusManager        │
└───────────────┬───────────────────────────────────────┬────────────────┘
                │                                       │
┌───────────────▼───────────────┐       ┌───────────────▼────────────────┐
│         NearbyManager         │       │          LynqDatabase          │
│   Strategy.P2P_CLUSTER        │       │  SQLite Messages, Peers, Queue │
│   Advertise / Discover / Send │       │  Seen-Set Deduplication Cache  │
└───────────────┬───────────────┘       └────────────────────────────────┘
                │
┌───────────────▼───────────────┐
│         CryptoManager         │
│  Hardware Keystore + ECDSA    │
│  E2EE (ECDH + AES-256-GCM)    │
└───────────────────────────────┘
```

---

## 2. Layers & Responsibilities

| Component | Responsibility |
|---|---|
| `NearbyManager` | Manages Google Nearby Connections lifecycle under `Strategy.P2P_CLUSTER`. Advertises local endpoint, discovers reachable peers, maintains bidirectional channels, and handles raw byte transmission. |
| `MessageRouter` | Executes the store-and-forward mesh routing algorithm: seen-set deduplication, TTL decrements, priority-ordered queuing, multi-hop flooding, and application-level ACK returns. |
| `CryptoManager` | Hardware-backed cryptographic identity (Android Keystore / EC secp256r1), ECDSA signatures for message authenticity, and AES-256-GCM encryption for 1-to-1 chats. |
| `LynqDatabase` | SQLite persistence layer for messages, peer directory, deduplication cache (`seen_messages`), and outgoing queue (`pending_queue`). |
| `LocationProvider` | Safe, non-blocking GPS provider using Google Play Services Fused Location. Attaches coordinates to SOS broadcasts without ever blocking transmission. |
| `LynqMeshService` | Foreground service keeping Nearby Connections and background relays active when the device screen locks. |
| `LynqViewModel` | Exposes reactive `StateFlow` streams (`peers`, `messages`, `alerts`, `pendingMessages`) to Compose screens. |

---

## 3. Cryptography & Identity

1. **Hardware Keystore Identity**: Every device generates an EC (secp256r1) keypair upon initial launch. The device ID is a 16-hex-digit fingerprint of the public key formatted as `xxxx xxxx xxxx xxxx`.
2. **SOS Broadcasts**: Unencrypted so that all intermediate relay devices and emergency responders can immediately inspect category, details, and GPS coordinates without prior key exchange. Authenticated via sender's ECDSA signature.
3. **1-to-1 CHAT Messages**: End-to-end encrypted using ECDH key agreement deriving a 256-bit symmetric AES-GCM key. Intermediary mesh relay nodes only forward encrypted ciphertext and routing headers.
