# PR Submission Guide & Template — TEAM-40 (FLUX)

### Direct GitHub Pull Request Link:
👉 **[Click Here to Open PR on GitHub](https://github.com/cbitosc/HFT26-TEAM-40/compare/main...Sunlesswastaken:HFT26-TEAM-40:main?expand=1)**

---

### PR Title:
```text
[SUBMISSION] TEAM-40 - FLUX - Offline Emergency Messaging System
```

---

### PR Description:
```markdown
# 🚀 Final Hackathon Submission — TEAM-40 (FLUX)

### 📋 Team Information
- **Team Name**: FLUX (Project: **Lynq**)
- **Team Number**: `TEAM-40` (`HFT26-TEAM-40`)
- **Registration ID**: `HFEST26-GJ9WMSBH`
- **Problem Track**: **Offline Emergency Messaging System**
- **Domain**: General Track
- **Team Leader**: G N SREEKAR (sreekargn485@gmail.com)
- **Institution**: INSTITUTE OF AERONAUTICAL ENGINEERING
- **Team Members**:
  1. Kamegaonkar Kruthik (kamegaonkarkruthik@gmail.com)
  2. Ashwik Saxena (saxenaashwik@gmail.com)
  3. VVM Sai Ganesh (vvmsaiganesh@gmail.com)

---

## 📌 Solution Overview & Approach
During natural disasters (cyclones, floods, earthquakes) or cellular infrastructure outages, traditional communication completely fails. 

**Lynq** is an **offline-first peer-to-peer (P2P) emergency mesh communication system** for Android:
- **Decentralized Multi-Hop Mesh**: Devices discover each other locally over Wi-Fi Direct and BLE (`Google Nearby Connections - P2P_CLUSTER`) and dynamically forward messages hop-by-hop ($A \to B \to C \to D$) without cellular towers, SIM cards, or internet routers.
- **Critical SOS Flooding**: User-triggered SOS alerts bypass normal queuing, auto-attach GPS coordinates, and broadcast unconditionally across every connected node.
- **End-to-End Encryption (E2EE)**: Direct chat messages are encrypted via AES-256-GCM using keys stored in the hardware-backed Android KeyStore; intermediate relay nodes forward packets without reading plaintext.
- **Multi-lingual AI Triage & Translation**: When any node encounters an internet uplink, an OpenAI-compatible FastAPI backend powered by Qwen 2.5 7B summarizes situational reports and translates messages across 6+ languages (Japanese, German, Bengali, Hindi, Spanish, French). Fully offline rule-based fallbacks ensure continuous operation with zero connectivity.

---

## 🏗️ Architecture & Tech Stack

```
┌──────────────────────────────── UI (Jetpack Compose) ───────────────────────────────┐
│  Network Dashboard  │  Encrypted Chats  │  Emergency SOS  │  Mesh Topology Explorer  │
└───────────────────────────────────────────┬──────────────────────────────────────────┘
                                            │ StateFlow ViewModels
┌───────────────────────────────────────────▼──────────────────────────────────────────┐
│                                 Messaging Engine                                     │
│     MessageRouter (TTL + Dedupe)  ·  PriorityQueue  ·  Android Keystore (E2EE)       │
└─────────────────────┬────────────────────────────────────────────┬───────────────────┘
                      │                                            │
        ┌─────────────▼───────────────┐              ┌─────────────▼─────────────┐
        │        NearbyManager        │              │       Room Database       │
        │  BLE / Wi-Fi Direct Cluster │              │ Messages · Peers · SeenSet│
        └─────────────────────────────┘              └───────────────────────────┘
                                       │ (Internet only, decoupled)
                         ┌─────────────▼─────────────┐
                         │   Lynq AI Cloud Backend   │
                         │    FastAPI + Qwen2.5-7B   │
                         └───────────────────────────┘
```

- **Mobile Client**: Kotlin, Jetpack Compose (Material 3 Dark Command-Center Theme), Android Coroutines & Flow, Google Nearby Connections API (`Strategy.P2P_CLUSTER`), Room SQLite DB, Android KeyStore + AES-256-GCM.
- **Cloud AI Backend**: Python 3.11, FastAPI, Uvicorn, Featherless AI (`Qwen/Qwen2.5-7B-Instruct`), deployed on Render.

---

## 🎥 Demo Video & Live Deployment Links

- **📹 High-Definition Demo Video**: [Watch Demo Video (docs/lynq-demo-1080p.mp4)](docs/lynq-demo-1080p.mp4)
- **☁️ Live AI Cloud Backend**: [`https://lynq-ai-backend.onrender.com`](https://lynq-ai-backend.onrender.com)
- **📚 API Documentation (Swagger)**: [`https://lynq-ai-backend.onrender.com/docs`](https://lynq-ai-backend.onrender.com/docs)

---

## 📸 Key Screenshots & Visuals

| 1. Emergency Command Dashboard | 2. Priority & Encrypted Chats |
|:---:|:---:|
| ![Dashboard](docs/screenshots/1-dashboard.png) | ![Messages](docs/screenshots/2-messages.png) |
| **3. One-Touch SOS Emergency Alert** | **4. Live Mesh Topology Explorer** |
| ![Emergency SOS](docs/screenshots/3-emergency.png) | ![Mesh Explorer](docs/screenshots/4-mesh.png) |

---

## 🚀 Local Setup & Installation Instructions

### 📱 Android Application Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/Sunlesswastaken/HFT26-TEAM-40.git
   cd HFT26-TEAM-40
   ```
2. Build and run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. Assemble the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install on a physical Android device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### 🐍 AI Backend Setup (Optional Local Run)
```bash
cd backend
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env # provide FEATHERLESS_API_KEY
uvicorn main:app --reload --port 8000
```

---

## 🔬 Submission Verification Checklist
- [x] Solution Overview & Approach documented
- [x] Architecture & Tech Stack detailed
- [x] Local setup & build instructions provided
- [x] Demo video link included
- [x] Screenshots and live UI visuals embedded
- [x] All automated unit tests passing (`./gradlew testDebugUnitTest`)
```
