package com.smartlifestyle.companion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** AI Coach chat history, kept locally only (like sensor data) rather than synced
 * to Firestore - conversation history is device-local by design here, not a
 * cross-device feature in this version. */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,       // "USER" | "COACH"
    val text: String,
    val timestampMillis: Long
)
