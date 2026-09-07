package com.smartlifestyle.companion.data.repository

import com.smartlifestyle.companion.BuildConfig
import com.smartlifestyle.companion.data.local.dao.ChatDao
import com.smartlifestyle.companion.data.local.entity.ChatMessageEntity
import com.smartlifestyle.companion.data.remote.ChatCompletionMessage
import com.smartlifestyle.companion.data.remote.ChatCompletionRequest
import com.smartlifestyle.companion.data.remote.RetrofitClient
import com.smartlifestyle.companion.domain.model.ChatMessage
import com.smartlifestyle.companion.domain.model.ChatSender
import com.smartlifestyle.companion.domain.model.LifestyleSnapshot
import com.smartlifestyle.companion.domain.usecase.AiCoachEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sends a user message to a real hosted LLM if AI_API_KEY is configured (see
 * local.properties / README), otherwise - and always as a fallback if that call
 * fails or times out - answers using the on-device AiCoachEngine so the coach
 * never goes silent. Every exchange is persisted locally via ChatDao regardless
 * of which path answered it.
 */
class AiCoachRepository(
    private val chatDao: ChatDao,
    private val engine: AiCoachEngine = AiCoachEngine()
) {
    fun observeMessages(): Flow<List<ChatMessage>> = chatDao.observeMessages().map { list ->
        list.map { ChatMessage(it.id, ChatSender.valueOf(it.sender), it.text, it.timestampMillis) }
    }

    suspend fun sendMessage(userText: String, snapshot: LifestyleSnapshot) {
        chatDao.insert(ChatMessageEntity(sender = ChatSender.USER.name, text = userText, timestampMillis = System.currentTimeMillis()))

        val reply = remoteReplyOrNull(userText, snapshot) ?: engine.reply(userText, snapshot)

        chatDao.insert(ChatMessageEntity(sender = ChatSender.COACH.name, text = reply, timestampMillis = System.currentTimeMillis()))
    }

    private suspend fun remoteReplyOrNull(userText: String, snapshot: LifestyleSnapshot): String? {
        val apiKey = BuildConfig.AI_API_KEY
        if (apiKey.isBlank()) return null

        return try {
            withTimeoutOrNull(REMOTE_TIMEOUT_MS) {
                val systemPrompt = "You are a concise lifestyle coach inside a mobile app. " +
                    "User's current stats: ${snapshot.stepsToday} steps, " +
                    "${snapshot.waterGlassesToday} glasses of water, " +
                    "heart rate ${snapshot.heartRateBpm?.toInt() ?: "unknown"} bpm, " +
                    "${snapshot.tasksRemaining} tasks remaining (${snapshot.tasksOverdue} overdue), " +
                    "weather ${snapshot.temperatureCelsius?.toInt() ?: "unknown"}C" +
                    (if (snapshot.isRaining) " raining" else "") +
                    ". Keep replies to 2-3 short sentences."

                val response = RetrofitClient.aiChatApi.getChatCompletion(
                    bearerToken = "Bearer $apiKey",
                    request = ChatCompletionRequest(
                        messages = listOf(
                            ChatCompletionMessage(role = "system", content = systemPrompt),
                            ChatCompletionMessage(role = "user", content = userText)
                        )
                    )
                )
                response.choices.firstOrNull()?.message?.content
            }
        } catch (e: Exception) {
            null // fall back to the on-device engine - see class doc
        }
    }

    fun personalizedAdvice(snapshot: LifestyleSnapshot): List<String> = engine.personalizedAdvice(snapshot)
    fun dailyPlan(snapshot: LifestyleSnapshot): List<String> = engine.dailyPlan(snapshot)

    companion object {
        private const val REMOTE_TIMEOUT_MS = 8000L
    }
}
