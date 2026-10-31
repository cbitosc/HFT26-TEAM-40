package com.sunless.lynq.ai

import com.sunless.lynq.domain.model.DeliveryStatus
import com.sunless.lynq.domain.model.LynqMessage
import com.sunless.lynq.domain.model.MessageType
import com.sunless.lynq.domain.model.Priority
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatherlessAiTest {

    @Test
    fun testLanguageRegistryLookup() {
        val hindi = LanguageRegistry.find("hi")
        assertEquals("Hindi", hindi.displayName)
        assertEquals("🇮🇳", hindi.flag)

        val spanish = LanguageRegistry.find("Spanish")
        assertEquals("es", spanish.code)
        assertEquals("🇪🇸", spanish.flag)

        val fallback = LanguageRegistry.find("UnknownLanguage")
        assertEquals("en", fallback.code)
    }

    @Test
    fun testOfflineHeuristicMedicalTriage() = runBlocking {
        val messages = listOf(
            LynqMessage(
                messageId = "msg-1",
                type = MessageType.CHAT,
                priority = Priority.HIGH,
                senderId = "peer-aarav",
                senderName = "Aarav",
                body = "We have an injured survivor with deep cuts. Urgent medical assistance required!",
                status = DeliveryStatus.DELIVERED,
            )
        )

        val client = FeatherlessAiClient(
            preferences = MockAiConfigProvider(key = "", lang = "Hindi")
        )

        val result = client.summarizeConversation("Aarav", messages, "Hindi")
        assertTrue("Result should be successful", result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals("HIGH", summary?.triageLevel)
        assertEquals("Medical", summary?.category)
        assertTrue("Summary should be marked as offline estimate", summary?.isOfflineEstimate == true)
    }

    @Test
    fun testOfflineHeuristicSafeStatusTriage() = runBlocking {
        val messages = listOf(
            LynqMessage(
                messageId = "msg-2",
                type = MessageType.CHAT,
                priority = Priority.NORMAL,
                senderId = "peer-priya",
                senderName = "Priya",
                body = "We are all safe here at the east shelter. We have water and battery packs.",
                status = DeliveryStatus.DELIVERED,
            )
        )

        val client = FeatherlessAiClient(
            preferences = MockAiConfigProvider(key = "", lang = "English")
        )

        val result = client.summarizeConversation("Priya", messages, "English")
        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals("SAFE", summary?.triageLevel)
        assertTrue(summary?.keyPoints?.any { it.contains("shelter", ignoreCase = true) } == true)
    }

    @Test
    fun testOfflineFallbackTranslation() = runBlocking {
        val client = FeatherlessAiClient(
            preferences = MockAiConfigProvider(key = "", lang = "Hindi")
        )

        val original = "Are you all safe? Cellular and internet are completely down here."
        val result = client.translateText(original, "Hindi")
        assertTrue(result.isSuccess)
        val translated = result.getOrNull()
        assertNotNull(translated)
        assertTrue("Translated text should contain Hindi devanagari characters", translated!!.contains("सुरक्षित"))
    }
    @Test
    fun testBackendConfigurationReadiness() {
        val provider = MockAiConfigProvider(
            key = "",
            baseUrl = "https://lynq-ai-backend.onrender.com",
            lang = "Hindi"
        )
        assertTrue("Backend server manages auth, so provider reports ready", provider.hasApiKey())
        assertEquals("https://lynq-ai-backend.onrender.com", provider.getConfig().baseUrl)
    }
}

private class MockAiConfigProvider(
    private val key: String = "",
    private val baseUrl: String = "https://api.featherless.ai/v1",
    private val lang: String = "Hindi",
) : AiConfigProvider {
    override fun getConfig(): AiConfig = AiConfig(
        apiKey = key,
        baseUrl = baseUrl,
        model = "Qwen/Qwen2.5-7B-Instruct",
        targetLanguage = lang,
    )
    override fun hasApiKey(): Boolean = key.isNotBlank() || (!baseUrl.contains("api.featherless.ai") && baseUrl.isNotBlank())
}
