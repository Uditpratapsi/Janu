package com.sukoon.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig

/**
 * Local-only AI runtime. No API key and no network request is made when asking questions.
 * The model file is downloaded once by MainActivity and then stays in app-private storage.
 */
object LocalModelRunner {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    @JvmStatic
    @Synchronized
    fun isLoaded(): Boolean = conversation != null

    @JvmStatic
    @Synchronized
    fun load(modelPath: String) {
        closeInternal()
        val nextEngine = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU()
            )
        )
        nextEngine.initialize()
        val nextConversation = nextEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(
                    "You are Sukoon (सुकून), a warm, caring, helpful Hindi-speaking assistant. " +
                    "Address the user kindly as jaanu when natural. Answer mainly in simple Hindi. " +
                    "Be accurate and honest. If you do not know something, say so. " +
                    "You are running offline on an Android phone, so do not claim to know live news, " +
                    "current weather, or facts that require internet access."
                ),
                samplerConfig = SamplerConfig(topK = 10, topP = 0.9, temperature = 0.7)
            )
        )
        engine = nextEngine
        conversation = nextConversation
    }

    @JvmStatic
    @Synchronized
    fun ask(prompt: String): String {
        val active = conversation ?: throw IllegalStateException("पहले AI मॉडल डाउनलोड करके तैयार करें।")
        val response = active.sendMessage(prompt)
        return response.text.trim().ifEmpty { "जानू, इस सवाल का जवाब अभी नहीं बना पाई। एक बार आसान शब्दों में पूछो।" }
    }

    @JvmStatic
    @Synchronized
    fun close() {
        closeInternal()
    }

    private fun closeInternal() {
        try { conversation?.close() } catch (_: Exception) {}
        conversation = null
        try { engine?.close() } catch (_: Exception) {}
        engine = null
    }
}
