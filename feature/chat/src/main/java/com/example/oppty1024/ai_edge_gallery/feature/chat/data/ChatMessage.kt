package com.example.oppty1024.ai_edge_gallery.feature.chat.data

import android.graphics.Bitmap

/**
 * 聊天消息的基础类
 */
sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long
    abstract val side: ChatSide
}

/**
 * 聊天消息的发送方
 */
enum class ChatSide {
    USER,    // 用户
    AGENT    // AI助手
}

/**
 * 文本消息
 */
data class ChatMessageText(
    override val id: String = generateId(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val side: ChatSide,
    val content: String,
    val isStreaming: Boolean = false,
    val accelerator: String = ""
) : ChatMessage() {
    fun clone(): ChatMessageText = copy(id = generateId(), timestamp = System.currentTimeMillis())
}

/**
 * 图片消息
 */
data class ChatMessageImage(
    override val id: String = generateId(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val side: ChatSide,
    val bitmaps: List<Bitmap>
) : ChatMessage()

/**
 * 加载状态消息
 */
data class ChatMessageLoading(
    override val id: String = generateId(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val side: ChatSide = ChatSide.AGENT,
    val accelerator: String = ""
) : ChatMessage()

/**
 * 警告消息
 */
data class ChatMessageWarning(
    override val id: String = generateId(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val side: ChatSide = ChatSide.AGENT,
    val content: String
) : ChatMessage()

/**
 * 信息消息
 */
data class ChatMessageInfo(
    override val id: String = generateId(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val side: ChatSide = ChatSide.AGENT,
    val content: String
) : ChatMessage()

/**
 * 生成唯一ID
 */
private fun generateId(): String = System.nanoTime().toString()
