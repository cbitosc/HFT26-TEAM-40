package com.sunless.lynq.ai

import com.sunless.lynq.domain.model.LynqMessage
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for Featherless AI (OpenAI-compatible LLM endpoint).
 * Provides decoupled situational summarization, triage classification, and translation.
 *
 * Per architecture rules:
 * - Operates only when internet is available.
 * - Fully decoupled: network mesh routing never blocks or waits on AI.
 * - Gracefully falls back to local heuristic analysis when offline or when no API key is provided.
 */
class FeatherlessAiClient(private val preferences: AiConfigProvider) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Summarizes a conversation thread between the local user and a peer.
     */
    suspend fun summarizeConversation(
        peerName: String,
        messages: List<LynqMessage>,
        targetLanguage: String? = null,
    ): Result<ThreadSummary> = withContext(Dispatchers.IO) {
        val config = preferences.getConfig()
        val lang = targetLanguage ?: config.targetLanguage

        if (messages.isEmpty()) {
            return@withContext Result.success(
                ThreadSummary(
                    overview = "No messages exchanged with $peerName yet.",
                    triageLevel = "SAFE",
                    category = "Status Update",
                    keyPoints = listOf("Channel established and end-to-end encrypted."),
                    actionableAdvice = "Standby for incoming mesh traffic.",
                    modelUsed = "Lynq Local Engine",
                    language = lang,
                    isOfflineEstimate = true,
                )
            )
        }

        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val isDirectFeatherless = cleanBaseUrl.contains("api.featherless.ai")

        // If explicitly pointing to raw Featherless API with no key, provide local heuristic
        if (isDirectFeatherless && config.apiKey.isBlank()) {
            return@withContext Result.success(
                buildLocalHeuristicSummary(peerName, messages, lang, reason = "Local Heuristic (Featherless API key not set)")
            )
        }

        try {
            if (!isDirectFeatherless) {
                // Call Lynq Render backend service (API key handled server-side)
                val endpoint = "$cleanBaseUrl/v1/summarize"
                val backendItems = messages.takeLast(15).map { msg ->
                    BackendMessageItem(
                        sender = if (msg.senderName == "You") "Local User" else peerName,
                        body = msg.body,
                        timestamp = msg.createdAt,
                    )
                }
                val request = BackendSummarizeRequest(
                    peer_name = peerName,
                    messages = backendItems,
                    target_language = lang,
                    model = config.model,
                )
                val responseString = executePost(
                    endpointUrl = endpoint,
                    apiKey = config.apiKey,
                    jsonBody = json.encodeToString(BackendSummarizeRequest.serializer(), request),
                )
                val response = json.decodeFromString(ThreadSummary.serializer(), responseString)
                Result.success(response.copy(language = lang))
            } else {
                // Direct Featherless OpenAI-compatible call
                val transcript = buildTranscript(peerName, messages)
                val systemPrompt = """
                    You are Lynq AI, an emergency response intelligence engine in a disconnected disaster mesh network.
                    Analyze this peer-to-peer chat thread and produce situational intelligence.
                    Output strictly raw JSON matching this schema (do NOT wrap in markdown or backticks):
                    {
                      "overview": "1-2 sentence core situation summary",
                      "triageLevel": "CRITICAL" or "HIGH" or "NORMAL" or "SAFE",
                      "category": "Medical" or "Rescue/Trapped" or "Fire" or "Logistics" or "Status Update",
                      "keyPoints": ["concise fact or need 1", "concise fact or need 2"],
                      "actionableAdvice": "1 sentence practical advice for responders or user"
                    }
                    Provide the text in $lang.
                """.trimIndent()

                val request = OpenAiChatRequest(
                    model = config.model,
                    messages = listOf(
                        OpenAiMessage(role = "system", content = systemPrompt),
                        OpenAiMessage(role = "user", content = "Conversation transcript:\n$transcript"),
                    ),
                    temperature = 0.2,
                    maxTokens = 450,
                )

                val endpoint = "${config.baseUrl}/chat/completions"
                val responseString = executePost(endpoint, config.apiKey, json.encodeToString(OpenAiChatRequest.serializer(), request))

                val response = json.decodeFromString(OpenAiChatResponse.serializer(), responseString)
                val assistantContent = response.choices.firstOrNull()?.message?.content?.trim()
                    ?: throw IllegalStateException("Empty completion from Featherless API")

                val parsedSummary = parseSummaryJson(assistantContent, config.model, lang)
                Result.success(parsedSummary)
            }
        } catch (e: Exception) {
            // Decoupled fallback: on network failure or timeout, return local heuristic estimate
            val fallback = buildLocalHeuristicSummary(
                peerName = peerName,
                messages = messages,
                language = lang,
                reason = "Local Heuristic (Offline / ${e.localizedMessage ?: "Connection error"})",
            )
            Result.success(fallback)
        }
    }

    /**
     * Translates a single text or message into the target language using Featherless AI.
     */
    suspend fun translateText(
        text: String,
        targetLanguage: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        val config = preferences.getConfig()
        val cleanText = text.trim()
        if (cleanText.isBlank()) return@withContext Result.success("")

        val cleanBaseUrl = config.baseUrl.trim().removeSuffix("/")
        val isDirectFeatherless = cleanBaseUrl.contains("api.featherless.ai")

        // If explicitly pointing to raw Featherless API with no key, provide offline stub
        if (isDirectFeatherless && config.apiKey.isBlank()) {
            return@withContext Result.success(localFallbackTranslate(cleanText, targetLanguage))
        }

        try {
            if (!isDirectFeatherless) {
                // Call Lynq Render backend service which holds the Featherless API key server-side
                val endpoint = "$cleanBaseUrl/v1/translate"
                android.util.Log.d("LynqAI", "Translating via Render backend: $endpoint to $targetLanguage")
                val request = BackendTranslateRequest(
                    text = cleanText,
                    target_language = targetLanguage,
                    model = config.model,
                )
                val responseString = executePost(
                    endpointUrl = endpoint,
                    apiKey = config.apiKey,
                    jsonBody = json.encodeToString(BackendTranslateRequest.serializer(), request),
                )
                val response = json.decodeFromString(BackendTranslateResponse.serializer(), responseString)
                val translated = response.translated.removePrefix("\"").removeSuffix("\"").trim()
                Result.success(translated.ifBlank { localFallbackTranslate(cleanText, targetLanguage) })
            } else {
                // Direct Featherless OpenAI-compatible call
                val systemPrompt = "You are Lynq Emergency Translation Engine. Translate the following text directly into $targetLanguage. Preserve all names, coordinates, and emergency terms. Output ONLY the translated text without commentary, prefixes, or quotation marks."

                val request = OpenAiChatRequest(
                    model = config.model,
                    messages = listOf(
                        OpenAiMessage(role = "system", content = systemPrompt),
                        OpenAiMessage(role = "user", content = cleanText),
                    ),
                    temperature = 0.1,
                    maxTokens = 250,
                )

                val endpoint = "$cleanBaseUrl/chat/completions"
                val responseString = executePost(endpoint, config.apiKey, json.encodeToString(OpenAiChatRequest.serializer(), request))

                val response = json.decodeFromString(OpenAiChatResponse.serializer(), responseString)
                val translated = response.choices.firstOrNull()?.message?.content?.trim()
                    ?: throw IllegalStateException("Empty translation response")

                // Clean up any extraneous quotes
                val cleaned = translated.removePrefix("\"").removeSuffix("\"").trim()
                Result.success(cleaned)
            }
        } catch (e: Exception) {
            android.util.Log.w("LynqAI", "Translation fallback used: ${e.localizedMessage}")
            Result.success(localFallbackTranslate(cleanText, targetLanguage))
        }
    }

    /**
     * Generates a mesh-wide situational report (SitRep) across all peer communications.
     */
    suspend fun generateMeshSitRep(
        allMessages: List<LynqMessage>,
        targetLanguage: String? = null,
    ): Result<ThreadSummary> = withContext(Dispatchers.IO) {
        val config = preferences.getConfig()
        val lang = targetLanguage ?: config.targetLanguage

        if (allMessages.isEmpty()) {
            return@withContext Result.success(
                ThreadSummary(
                    overview = "No mesh traffic recorded yet. Network is standing by.",
                    triageLevel = "NORMAL",
                    category = "Status Update",
                    keyPoints = listOf("Mesh discovery active", "0 packets in store-and-forward queue"),
                    actionableAdvice = "Keep app open to relay packets for nearby survivors.",
                    modelUsed = "Lynq Local Engine",
                    language = lang,
                    isOfflineEstimate = true,
                )
            )
        }

        summarizeConversation("All Mesh Nodes", allMessages.takeLast(20), lang)
    }

    private fun executePost(endpointUrl: String, apiKey: String, jsonBody: String): String {
        val url = URL(endpointUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 25_000
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            ?: throw IllegalStateException("HTTP $code without error details")

        val responseText = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

        if (code !in 200..299) {
            throw IllegalStateException("Featherless API error (HTTP $code): $responseText")
        }

        return responseText
    }

    private fun buildTranscript(peerName: String, messages: List<LynqMessage>): String {
        val sb = StringBuilder()
        messages.takeLast(15).forEach { msg ->
            val sender = if (msg.senderName == "You") "Local User" else peerName
            sb.appendLine("[$sender]: ${msg.body}")
        }
        return sb.toString()
    }

    private fun parseSummaryJson(rawText: String, modelName: String, language: String): ThreadSummary {
        // Strip markdown backticks if model wrapped JSON in ```json ... ```
        val cleaned = rawText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val element = json.parseToJsonElement(cleaned).jsonObject
            val overview = element["overview"]?.jsonPrimitive?.content ?: "Conversation summary generated."
            val triageLevel = element["triageLevel"]?.jsonPrimitive?.content?.uppercase() ?: "NORMAL"
            val category = element["category"]?.jsonPrimitive?.content ?: "Status Update"
            val advice = element["actionableAdvice"]?.jsonPrimitive?.content ?: "Maintain regular mesh contact."

            val points = mutableListOf<String>()
            element["keyPoints"]?.let { arrayElement ->
                try {
                    if (arrayElement is kotlinx.serialization.json.JsonArray) {
                        arrayElement.forEach { pt ->
                            points.add(pt.jsonPrimitive.content)
                        }
                    }
                } catch (_: Exception) {}
            }
            if (points.isEmpty()) {
                points.add("Direct encrypted communication established.")
            }

            ThreadSummary(
                overview = overview,
                triageLevel = triageLevel,
                category = category,
                keyPoints = points,
                actionableAdvice = advice,
                modelUsed = modelName,
                language = language,
                isOfflineEstimate = false,
            )
        } catch (_: Exception) {
            // In case of non-JSON LLM response, use raw text as overview
            ThreadSummary(
                overview = cleaned.lines().firstOrNull() ?: cleaned,
                triageLevel = if (cleaned.contains("emergency", true) || cleaned.contains("urgent", true)) "HIGH" else "NORMAL",
                category = "Status Update",
                keyPoints = cleaned.lines().drop(1).filter { it.isNotBlank() }.take(3).ifEmpty { listOf("Active mesh conversation") },
                actionableAdvice = "Review latest incoming messages.",
                modelUsed = modelName,
                language = language,
                isOfflineEstimate = false,
            )
        }
    }

    /**
     * Local heuristic summary for offline use or when API key is not yet provided.
     */
    private fun buildLocalHeuristicSummary(
        peerName: String,
        messages: List<LynqMessage>,
        language: String,
        reason: String,
    ): ThreadSummary {
        val allText = messages.joinToString(" ") { it.body }.lowercase()
        val hasSos = messages.any { it.type == MessageType.SOS || it.priority == Priority.CRITICAL }

        val hasMedical = allText.contains("medical") || allText.contains("injury") || allText.contains("hurt") || allText.contains("doctor") || allText.contains("blood")
        val hasRescue = allText.contains("trap") || allText.contains("rescue") || allText.contains("collapsed") || allText.contains("under rubble")
        val hasFire = allText.contains("fire") || allText.contains("smoke") || allText.contains("burning")
        val isSafe = allText.contains("safe") || allText.contains("fine") || allText.contains("all good") || allText.contains("east shelter")

        val (triageLevel, category) = when {
            hasSos || hasRescue -> "CRITICAL" to "Rescue/Trapped"
            hasMedical -> "HIGH" to "Medical"
            hasFire -> "CRITICAL" to "Fire"
            isSafe -> "SAFE" to "Status Update"
            allText.contains("urgent") || allText.contains("help") -> "HIGH" to "Emergency"
            else -> "NORMAL" to "Logistics"
        }

        val keyPoints = mutableListOf<String>()
        if (isSafe) keyPoints.add("Reported safe; gathered at local shelter/station")
        if (allText.contains("battery") || allText.contains("power")) keyPoints.add("Battery/power packs mentioned")
        if (allText.contains("relay") || allText.contains("mesh")) keyPoints.add("Mesh forwarding and relay nodes active")
        if (hasRescue) keyPoints.add("Urgent extraction / rescue assistance requested")
        if (hasMedical) keyPoints.add("Medical supplies or first-aid triage required")
        if (keyPoints.isEmpty()) {
            keyPoints.add("${messages.size} encrypted messages exchanged with $peerName")
        }

        val overview = when {
            isSafe -> "Participants report safe status. Mesh connection established with active relay hops."
            hasRescue -> "Urgent rescue assistance requested in this thread. Prioritize relay forwarding."
            hasMedical -> "Medical care needed. Relaying coordinates and status to all nearby responders."
            else -> "Active encrypted emergency communication with $peerName across ${messages.size} messages."
        }

        val advice = when (triageLevel) {
            "CRITICAL" -> "Keep Bluetooth/Wi-Fi radios active for uninterrupted multi-hop relay."
            "HIGH" -> "Acknowledge receipt and dispatch assistance when available."
            else -> "Maintain background relaying to assist nearby survivor network."
        }

        return ThreadSummary(
            overview = overview,
            triageLevel = triageLevel,
            category = category,
            keyPoints = keyPoints,
            actionableAdvice = advice,
            modelUsed = reason,
            language = language,
            isOfflineEstimate = true,
        )
    }

    /**
     * Local offline phrase translation for instant responsiveness when no key is set or offline.
     */
    private fun localFallbackTranslate(text: String, targetLanguage: String): String {
        val lower = text.trim().lowercase()
        return when (targetLanguage.lowercase()) {
            "hindi", "hi" -> when {
                lower.contains("are you all safe") || lower.contains("are you safe") ->
                    "क्या आप सब सुरक्षित हैं? यहाँ सेलुलर और इंटरनेट पूरी तरह बंद हैं।"
                lower.contains("we're safe") || lower.contains("we are safe") ->
                    "हम सुरक्षित हैं। हमने बैटरी पैक के साथ पूर्वी आश्रय में मेश नेटवर्क बनाया है।"
                lower.contains("got it, two more people just connected") || lower.contains("two more people") ->
                    "समझ गया, आरव के फोन के माध्यम से दो और लोग जुड़ गए हैं।"
                lower.contains("keep lynq running in background") ->
                    "लिंक (Lynq) को बैकग्राउंड में चालू रखें। सभी पैकेट अपने आप आगे रिले होंगे।"
                lower.contains("medical assistance required") || lower.contains("medical") ->
                    "चिकित्सा सहायता की आवश्यकता है!"
                lower.contains("trapped") ->
                    "फंसे हुए हैं, तत्काल सहायता की आवश्यकता है!"
                lower.contains("fire") ->
                    "आग का खतरा, कृपया सतर्क रहें!"
                else -> text
            }
            "japanese", "ja" -> when {
                lower.contains("are you all safe") || lower.contains("are you safe") ->
                    "皆さん安全ですか？ここでは携帯電話とインターネットが完全にダウンしています。"
                lower.contains("we're safe") || lower.contains("we are safe") ->
                    "私たちは無事です。バッテリーパックを持って東のシェルターでメッシュネットワークを構築しました。"
                lower.contains("two more people") ->
                    "了解しました。アーラヴのスマホを経由してさらに2人が接続しました。"
                lower.contains("background") ->
                    "Lynqをバックグラウンドで起動し続けてください。パケットは自動で中継されます。"
                lower.contains("medical") ->
                    "医療支援が必要です！至急対応願います。"
                lower.contains("trapped") ->
                    "閉じ込められています、至急救助が必要です！"
                lower.contains("fire") ->
                    "火災の危険があります、警戒してください！"
                else -> text
            }
            "bengali", "bn" -> when {
                lower.contains("are you all safe") || lower.contains("are you safe") ->
                    "আপনারা কি সবাই নিরাপদ? এখানে সেলুলার এবং ইন্টারনেট সম্পূর্ণ বন্ধ রয়েছে।"
                lower.contains("we're safe") || lower.contains("we are safe") ->
                    "আমরা নিরাপদ আছি। আমরা ব্যাটারি প্যাক নিয়ে পূর্ব আশ্রয়কেন্দ্রে মেশ নেটওয়ার্ক গড়ে তুলেছি।"
                lower.contains("two more people") ->
                    "বুঝেছি, আরভের ফোনের মাধ্যমে আরও দুইজন যুক্ত হয়েছে।"
                lower.contains("background") ->
                    "Lynq ব্যাকগ্রাউন্ডে চালু রাখুন। সমস্ত বার্তা স্বয়ংক্রিয়ভাবে রিলে হবে।"
                lower.contains("medical") ->
                    "জরুরি চিকিৎসা সহায়তার প্রয়োজন!"
                lower.contains("trapped") ->
                    "আটকে পড়েছি, অবিলম্বে উদ্ধার সাহায্য প্রয়োজন!"
                lower.contains("fire") ->
                    "আগুনের বিপদ, অনুগ্রহ করে সতর্ক থাকুন!"
                else -> text
            }
            "spanish", "es" -> when {
                lower.contains("are you safe") -> "¿Están todos a salvo? La red celular e internet están caídos."
                lower.contains("we're safe") || lower.contains("we are safe") -> "Estamos a salvo. Formamos una red mesh en el refugio este."
                lower.contains("medical") -> "¡Se requiere asistencia médica de emergencia!"
                lower.contains("trapped") -> "¡Atrapados, se requiere rescate urgente!"
                lower.contains("fire") -> "¡Peligro de incendio, manténgase alerta!"
                else -> text
            }
            "french", "fr" -> when {
                lower.contains("are you safe") -> "Êtes-vous tous en sécurité ? Le réseau cellulaire est en panne."
                lower.contains("we're safe") || lower.contains("we are safe") -> "Nous sommes en sécurité dans l'abri est."
                lower.contains("medical") -> "Assistance médicale urgente requise !"
                lower.contains("trapped") -> "Piégés, secours d'urgence demandés !"
                else -> text
            }
            "german", "de" -> when {
                lower.contains("are you safe") -> "Sind Sie alle in Sicherheit? Mobilfunk und Internet sind ausgefallen."
                lower.contains("we're safe") || lower.contains("we are safe") -> "Wir sind in Sicherheit in der östlichen Notunterkunft."
                lower.contains("medical") -> "Dringende medizinische Hilfe erforderlich!"
                else -> text
            }
            "arabic", "ar" -> when {
                lower.contains("are you safe") -> "هل الجميع بأمان؟ شبكة الهاتف والإنترنت معطلة تماماً هنا."
                lower.contains("we're safe") || lower.contains("we are safe") -> "نحن بأمان. شكلना شبكة مِش في المأوى الشرقي."
                lower.contains("medical") -> "مطلوب مساعدة طبية عاجلة!"
                else -> text
            }
            else -> text
        }
    }
}
