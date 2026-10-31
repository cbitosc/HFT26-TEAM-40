# Lynq AI Backend (Featherless AI Proxy)

FastAPI service that provides decoupled situational intelligence, crisis summarization, and multi-language emergency translation for the Lynq mesh network via Featherless.ai (OpenAI-compatible LLM endpoint).

## 🚀 1-Click Render Autodeploy (Blueprint)

This repository includes a [`render.yaml`](../render.yaml) blueprint specification for automatic deployment:

1. Go to **[dashboard.render.com/blueprints](https://dashboard.render.com/blueprints)**.
2. Click **New Blueprint Instance**.
3. Paste your GitHub repository link and select the branch (`main`).
4. Render will read `render.yaml` automatically and prompt for `FEATHERLESS_API_KEY`.
5. Paste your Featherless API key and click **Apply**.
6. Render builds, deploys, and provides an instant public HTTPS URL (e.g. `https://lynq-ai-backend.onrender.com`).

Copy your Render URL into the **API Base URL** setting in the Lynq Android app (`https://<your-service>.onrender.com/v1`).

---

## Local Setup & Run

1. **Virtual Environment & Dependencies:**
   ```bash
   cd backend
   python -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   ```

2. **Configure API Key:**
   ```bash
   cp .env.example .env
   # Edit .env and paste your Featherless API key
   ```

3. **Start Server:**
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

## Connecting with Lynq Android Client

- **On Android Emulator:** Point the Base URL in Lynq App Settings to: `http://10.0.2.2:8000/v1`
- **On Physical Device:** Point the Base URL to: `http://<YOUR_COMPUTER_LOCAL_IP>:8000/v1`
- **Or Direct Client Mode:** The Lynq Android app can also connect directly to `https://api.featherless.ai/v1` by pasting the API key into the app's Settings or Chat dialog.

## Available Endpoints

- `GET /health` — Check server status & Featherless key readiness
- `POST /v1/summarize` — Analyze conversation transcript & return structured triage & emergency summary
- `POST /v1/translate` — Translate crisis messages accurately into Hindi, Spanish, French, etc.
- `POST /v1/chat/completions` — Direct OpenAI-compatible endpoint proxy
