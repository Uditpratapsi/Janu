package com.sukoon.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig

/** Local-only AI runtime. The model is downloaded once; prompts run on-device. */
object LocalModelRunner {
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    @JvmStatic @Synchronized
    fun isLoaded(): Boolean = conversation != null

    @JvmStatic @Synchronized
    fun load(modelPath: String) {
        closeInternal()
        val nextEngine = Engine(EngineConfig(modelPath = modelPath, backend = Backend.CPU()))
        nextEngine.initialize()
        val nextConversation = nextEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(
                    "You are Sukoon (सुकून), a warm, caring Hindi-speaking assistant. " +
                    "Address the user kindly as jaanu when natural. Answer in simple Hindi. " +
                    "Be accurate and honest. This runs offline, so do not claim live news or weather."
                ),
                samplerConfig = SamplerConfig(topK = 10, topP = 0.9, temperature = 0.7)
            )
        )
        engine = nextEngine
        conversation = nextConversation
    }

    @JvmStatic @Synchronized
    fun ask(prompt: String): String {
        val active = conversation ?: throw IllegalStateException("पहले AI मॉडल डाउनलोड करके तैयार करें।")
        return active.sendMessage(prompt).toString().trim().ifEmpty {
            "जानू, इस सवाल का जवाब अभी नहीं बना पाई। एक बार आसान शब्दों में पूछो।"
        }
    }

    @JvmStatic @Synchronized
    fun close() { closeInternal() }

    private fun closeInternal() {
        try { conversation?.close() } catch (_: Exception) {}
        conversation = null
        try { engine?.close() } catch (_: Exception) {}
        engine = null
    }
}
