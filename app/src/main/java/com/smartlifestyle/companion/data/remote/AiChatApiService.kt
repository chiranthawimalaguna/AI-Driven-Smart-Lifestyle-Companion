package com.smartlifestyle.companion.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ChatCompletionMessage(val role: String, val content: String)
data class ChatCompletionRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatCompletionMessage>
)
data class ChatCompletionChoice(val message: ChatCompletionMessage)
data class ChatCompletionResponse(val choices: List<ChatCompletionChoice>)

/**
 * OPTIONAL real LLM backend for the AI Coach, wired to an OpenAI-compatible chat
 * completions endpoint. This is used only if AI_API_KEY is set in local.properties
 * (see BuildConfig.AI_API_KEY) - AiCoachRepository falls back to the on-device
 * AiCoachEngine automatically if no key is configured or the call fails, so the
 * app works fully without ever calling this. Not tested end-to-end against a live
 * key from this environment - verify it yourself once you add a real key.
 */
interface AiChatApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}
