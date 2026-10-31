import os
import json
from typing import List, Optional
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import httpx

load_dotenv()

FEATHERLESS_API_KEY = os.getenv("FEATHERLESS_API_KEY", "")
FEATHERLESS_BASE_URL = os.getenv("FEATHERLESS_BASE_URL", "https://api.featherless.ai/v1").rstrip("/")
DEFAULT_FALLBACK_MODEL = "Qwen/Qwen2.5-7B-Instruct"
FEATHERLESS_MODEL = os.getenv("FEATHERLESS_MODEL", DEFAULT_FALLBACK_MODEL)

app = FastAPI(
    title="Lynq Emergency Intelligence Backend",
    description="Decoupled AI assistant for Lynq disaster mesh network powered by Featherless AI.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class MessageItem(BaseModel):
    sender: str
    body: str
    timestamp: Optional[int] = None


class SummarizeRequest(BaseModel):
    peer_name: str = Field(default="Survivor")
    messages: List[MessageItem]
    target_language: str = Field(default="Hindi")
    model: Optional[str] = None


class ThreadSummaryResponse(BaseModel):
    overview: str
    triageLevel: str
    category: str
    keyPoints: List[str]
    actionableAdvice: str
    modelUsed: str
    language: str
    isOfflineEstimate: bool = False


class TranslateRequest(BaseModel):
    text: str
    target_language: str = Field(default="Hindi")
    model: Optional[str] = None


class TranslateResponse(BaseModel):
    original: str
    translated: str
    target_language: str
    modelUsed: str


class TriageRequest(BaseModel):
    message: str


class TriageResponse(BaseModel):
    category: str
    priority: str
    suggested_action: str


async def _execute_chat_with_fallback(
    client: httpx.AsyncClient,
    model: str,
    messages: list,
    max_tokens: int,
    temperature: float = 0.2,
) -> tuple[dict, str]:
    """
    Executes a chat completion call with automatic fallback to an ungated model
    (Qwen/Qwen2.5-7B-Instruct) if the selected model is gated (like Meta Llama) or fails with 403.
    """
    target_model = model
    # Meta Llama models on Featherless require HuggingFace OAuth gating verification.
    # Default automatically to Qwen to ensure guaranteed 100% uptime for hackathon demo.
    if "llama" in target_model.lower():
        target_model = DEFAULT_FALLBACK_MODEL

    payload = {
        "model": target_model,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
    }

    headers = {
        "Authorization": f"Bearer {FEATHERLESS_API_KEY}",
        "Content-Type": "application/json",
    }

    resp = await client.post(
        f"{FEATHERLESS_BASE_URL}/chat/completions",
        headers=headers,
        json=payload,
    )

    # If the requested model is gated or failed, fall back to ungated Qwen model
    if resp.status_code in (403, 400) and target_model != DEFAULT_FALLBACK_MODEL:
        payload["model"] = DEFAULT_FALLBACK_MODEL
        target_model = DEFAULT_FALLBACK_MODEL
        resp = await client.post(
            f"{FEATHERLESS_BASE_URL}/chat/completions",
            headers=headers,
            json=payload,
        )

    if resp.status_code != 200:
        raise HTTPException(status_code=resp.status_code, detail=resp.text)

    return resp.json(), target_model


@app.get("/")
def root():
    return {
        "service": "Lynq AI Emergency Intelligence Backend",
        "status": "online",
        "docs": "/docs",
        "health": "/health",
        "model": FEATHERLESS_MODEL,
        "endpoints": {
            "summarize": "/v1/summarize",
            "translate": "/v1/translate",
            "chat_completions": "/v1/chat/completions",
        },
    }


@app.get("/health")
def health():
    return {
        "status": "online",
        "service": "lynq-backend",
        "has_featherless_key": bool(FEATHERLESS_API_KEY and FEATHERLESS_API_KEY != "your_featherless_api_key_here"),
        "model": FEATHERLESS_MODEL,
    }


@app.post("/v1/summarize", response_model=ThreadSummaryResponse)
@app.post("/summarize", response_model=ThreadSummaryResponse)
async def summarize_thread(req: SummarizeRequest):
    if not req.messages:
        return ThreadSummaryResponse(
            overview="No messages to summarize.",
            triageLevel="SAFE",
            category="Status Update",
            keyPoints=["No active communication recorded."],
            actionableAdvice="Maintain standby relaying.",
            modelUsed=req.model or FEATHERLESS_MODEL,
            language=req.target_language,
            isOfflineEstimate=True,
        )

    transcript = "\n".join([f"[{m.sender}]: {m.body}" for m in req.messages[-15:]])

    system_prompt = f"""You are Lynq AI, an emergency response intelligence engine running in a disconnected disaster mesh network.
Analyze this peer-to-peer chat thread and produce situational intelligence.
Output strictly raw JSON matching this schema (do NOT wrap in markdown or code blocks):
{{
  "overview": "1-2 sentence core situation summary",
  "triageLevel": "CRITICAL" or "HIGH" or "NORMAL" or "SAFE",
  "category": "Medical" or "Rescue/Trapped" or "Fire" or "Logistics" or "Status Update",
  "keyPoints": ["fact or need 1", "fact or need 2"],
  "actionableAdvice": "1 sentence practical advice for responders or user"
}}
Provide the text in {req.target_language}.
"""

    model_to_use = req.model or FEATHERLESS_MODEL
    key_to_use = FEATHERLESS_API_KEY

    if not key_to_use or key_to_use == "your_featherless_api_key_here":
        return ThreadSummaryResponse(
            overview=f"Active emergency coordination with {req.peer_name}. Messages relayed across mesh.",
            triageLevel="NORMAL",
            category="Logistics",
            keyPoints=[f"{len(req.messages)} messages logged in thread", "Channel verified"],
            actionableAdvice="Keep Bluetooth active to forward survivor packets.",
            modelUsed="Local Backend Heuristic (No Featherless Key in .env)",
            language=req.target_language,
            isOfflineEstimate=True,
        )

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            data, used_model = await _execute_chat_with_fallback(
                client=client,
                model=model_to_use,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"Transcript:\n{transcript}"},
                ],
                max_tokens=400,
                temperature=0.2,
            )

        content = data["choices"][0]["message"]["content"].strip()
        cleaned = content.removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        parsed = json.loads(cleaned)

        raw_points = parsed.get("keyPoints", ["Encrypted connection active"])
        if isinstance(raw_points, list):
            points = [str(p) for p in raw_points]
        else:
            points = [str(raw_points)]

        raw_advice = parsed.get("actionableAdvice", "Maintain radio contact.")
        if isinstance(raw_advice, list):
            advice = " ".join(str(a) for a in raw_advice)
        else:
            advice = str(raw_advice)

        return ThreadSummaryResponse(
            overview=parsed.get("overview", "Thread summarized."),
            triageLevel=parsed.get("triageLevel", "NORMAL"),
            category=parsed.get("category", "Status Update"),
            keyPoints=points,
            actionableAdvice=advice,
            modelUsed=used_model,
            language=req.target_language,
            isOfflineEstimate=False,
        )
    except Exception as e:
        return ThreadSummaryResponse(
            overview=f"Automated situational summary for {req.peer_name}.",
            triageLevel="NORMAL",
            category="Status Update",
            keyPoints=["Relay operational", f"Offline estimate due to: {str(e)[:60]}"],
            actionableAdvice="Maintain regular mesh contact.",
            modelUsed="Local Heuristic Fallback",
            language=req.target_language,
            isOfflineEstimate=True,
        )


@app.post("/v1/translate", response_model=TranslateResponse)
@app.post("/translate", response_model=TranslateResponse)
async def translate_text(req: TranslateRequest):
    clean_text = req.text.strip()
    if not clean_text:
        return TranslateResponse(original="", translated="", target_language=req.target_language, modelUsed=FEATHERLESS_MODEL)

    model_to_use = req.model or FEATHERLESS_MODEL
    key_to_use = FEATHERLESS_API_KEY

    if not key_to_use or key_to_use == "your_featherless_api_key_here":
        return TranslateResponse(
            original=clean_text,
            translated=f"[{req.target_language}]: {clean_text}",
            target_language=req.target_language,
            modelUsed="Local Fallback",
        )

    try:
        system_prompt = f"You are Lynq Emergency Translation Engine. Translate the text directly into {req.target_language}. Preserve all numbers, coordinates, and emergency words. Output ONLY the translated text without commentary."

        async with httpx.AsyncClient(timeout=25.0) as client:
            data, used_model = await _execute_chat_with_fallback(
                client=client,
                model=model_to_use,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": clean_text},
                ],
                max_tokens=250,
                temperature=0.1,
            )

        translated = data["choices"][0]["message"]["content"].strip().strip('"')

        return TranslateResponse(
            original=clean_text,
            translated=translated,
            target_language=req.target_language,
            modelUsed=used_model,
        )
    except Exception as e:
        return TranslateResponse(
            original=clean_text,
            translated=f"[{req.target_language}]: {clean_text}",
            target_language=req.target_language,
            modelUsed=f"Fallback ({str(e)[:40]})",
        )


@app.post("/v1/chat/completions")
async def proxy_chat_completions(req_body: dict):
    """Transparent proxy to Featherless OpenAI-compatible endpoint."""
    key_to_use = FEATHERLESS_API_KEY
    if not key_to_use:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="FEATHERLESS_API_KEY not configured on backend.")

    async with httpx.AsyncClient(timeout=40.0) as client:
        resp = await client.post(
            f"{FEATHERLESS_BASE_URL}/chat/completions",
            headers={
                "Authorization": f"Bearer {key_to_use}",
                "Content-Type": "application/json",
            },
            json=req_body,
        )
        return resp.json()
