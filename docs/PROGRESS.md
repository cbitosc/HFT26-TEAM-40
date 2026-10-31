# Lynq — Progress

## Done

- **Project Skeleton & Architecture**:
  - Gradle 9.6 + Kotlin 2.2 + Compose + version catalog setup.
  - Strict decoupling of engine, networking, data, and UI layers.
  - Clean edge-to-edge support with custom dark theme.

- **Domain Models & Wire Protocol**:
  - Domain models matching AGENTS.md (`MessageType`, `Priority`, `DeliveryStatus`, `Peer`, `LynqMessage`).
  - Wire protocol model (`WireMessage`) serialized via `kotlinx.serialization` with TTL hop limits, route histories, and cryptographic signatures.

- **Nearby Connections Mesh (`Strategy.P2P_CLUSTER`)**:
  - `NearbyManager`: Background advertising, automatic endpoint discovery, mutual connection handshakes, and raw byte transfers.
  - Dynamic peer identity mapping with PRESENCE exchanges.

- **Store-and-Forward Routing Engine (`MessageRouter`)**:
  - Deduplication with persistent SQLite `seen_messages` table.
  - TTL hop decrementing and expiry enforcement.
  - Multi-hop broadcast flooding for emergency SOS.
  - Application-level ACK loopback (`A → B → C → D` and ACK `D → C → B → A`).
  - Priority queue (`CRITICAL` > `HIGH` > `NORMAL`).
  - Built-in multi-hop simulation harness for live demonstration on real devices and emulators.

- **Cryptographic Security & Identity (`CryptoManager`)**:
  - Android Keystore backed EC (P-256) keypair generation and verification.
  - Human-readable device identity fingerprint (`xxxx xxxx xxxx xxxx`).
  - SHA256withECDSA digital signatures for origin authenticity.
  - AES-256-GCM end-to-end encryption for 1-to-1 CHAT messages.
  - Plaintext SOS payloads with digital signatures for universal emergency readability.

- **Local Persistence Layer (`LynqDatabase`)**:
  - SQLite database persisting messages, reachable peers, deduplication seen-set, and priority queue.
  - Reactive `StateFlow` streams automatically updating UI.

- **Safe Location Provider (`LocationProvider`)**:
  - Non-blocking Google Play Services Fused Location Provider.
  - Never blocks SOS alerts if GPS is unavailable or permissions are denied.

- **Background Relay Service (`LynqMeshService`)**:
  - Foreground service with notification maintaining mesh links when the screen locks.

- **UI Overhaul & Apple Minimalist Polish**:
  - Fully bound to `LynqViewModel` across all 5 screens: Network Dashboard, Messages, Emergency SOS, Mesh Explorer, and Settings.
  - Interactive Obsidian knowledge graph with harmonic physics and multi-touch zoom (`MeshGraph.kt`).
  - Real-time SOS broadcast flow with live hop timeline and ACK confirmation.
  - Runtime permissions requester for Bluetooth, Nearby Wi-Fi, Location, and Notifications in `MainActivity`.
  - **Apple Concentric Navigation Dock (`CmdBar.kt`)**: Implemented strict concentric curvature geometry ($R_{outer}=28\text{dp}$, $4\text{dp}$ padding, $R_{inner}=24\text{dp}$) with subtle 0.5dp glass highlight border.
  - **Tactile 3-Second Hold SOS (`HoldToSosButton.kt`)**: 3-second hold-to-confirm prevention against accidental sends, featuring circular 360° charging countdown arc, smooth scale press compression, radiant horizontal progress fill, bottom charging bar, and live percentage readout.

- **Physical Device Screenshot Suite & Dynamic Appearance**:
  - Captured crisp 1224x2720 screenshots from connected physical Android hardware (`00386769G001856`):
    - `1-dashboard.png`: Live Network Dashboard with 5 active nodes and transmission feed.
    - `2-messages.png`: Encrypted channels list with 6 online peers and relay badges.
    - `state-thread.png`: iMessage-style E2EE conversation thread with multi-hop hops indicators.
    - `3-emergency.png` & `state-sos-tracking.png`: Critical SOS broadcast with 3-second hold and live hop relay pathway.
    - `4-mesh.png`: Obsidian-style interactive mesh constellation graph with SOS route highlights.
    - `5-settings.png`: Hardware keystore info, radio controls, and appearance toggle.
    - `light-dashboard.png`, `light-messages.png`, `light-emergency.png`, `light-mesh.png`, `light-settings.png`: Full Apple HIG light mode suite.

- **1080p Product Demo Video & QC Pipeline**:
  - Built custom programmatic video in Remotion (`demo-video/`):
    - 6 high-impact scenes: Problem & Architecture, Multi-Hop Mesh Topology, Critical SOS Broadcast, Encrypted Chat with ACKs, Apple HIG Light/Dark Design Craft, and Resilience CTA.
    - Realistic smartphone mockup frames with metallic chassis, dynamic island, and floating drift animation.
    - Ambient techno synth audio track generated via FFmpeg lavfi filters.
  - Rendered final deliverable: `docs/lynq-demo-1080p.mp4` (1920x1080, 50s, 30fps, H.264 / AAC, 10.9MB).
  - Passed `qc_video.py` with 0 failures and 0 warnings.
  - Generated visual review contact sheet: `docs/demo-contact-sheet.jpg`.

- **Featherless AI Situational Intelligence & Emergency Translation**:
  - **Decoupled Architecture**: Fully non-blocking AI layer in `com.sunless.lynq.ai` implementing OpenAI-compatible Featherless API (`api.featherless.ai/v1`).
  - **Thread Situational Summary Card**: Expandable intelligence card at the top of active chats extracting core situation overviews, triage urgency (`CRITICAL`, `HIGH`, `SAFE`, `NORMAL`), category classification, key location/resource points, and actionable responder advice.
  - **Multi-Language Emergency Translation Engine**:
    - Supports 8 disaster communication languages: English 🇺🇸, Hindi (हिन्दी) 🇮🇳, Spanish (Español) 🇪🇸, French (Français) 🇫🇷, German (Deutsch) 🇩🇪, Japanese (日本語) 🇯🇵, Arabic (العربية) 🇸🇦, and Bengali (বাংলা) 🇧🇩.
    - Inline on-demand translation for individual incoming and outgoing chat bubbles with toggle hide/show.
  - **Mesh SitRep (Situational Report)**: Network-wide synthesis card at the top of the Messages screen to aggregate and summarize packet traffic across all connected mesh nodes.
  - **Zero-Crash Offline Fallback**: Smart local emergency heuristic engine providing realistic triage estimates and localized translations even before the Featherless API key is entered or when completely offline.
  - **On-Device Key & Model Configuration**: Direct in-chat API key configuration modal + full Settings controls (API Key, Model picker with Llama-3.1-8B / Mistral / Qwen / Gemma, Base URL, and Default Language).
  - **Python FastAPI Backend (`backend/`)**: Standalone microservice proxying Featherless API with `/health`, `/v1/summarize`, `/v1/translate`, and `/v1/chat/completions`.
  - **Unit Test Coverage**: `FeatherlessAiTest` validating language registry lookups, heuristic medical triage classification, safe-status heuristics, and offline translations.

## Next steps

- Multi-device field verification on physical hardware across 4 devices.
- Video demo recording of multi-hop SOS packet propagation (`A → B → C → D`).