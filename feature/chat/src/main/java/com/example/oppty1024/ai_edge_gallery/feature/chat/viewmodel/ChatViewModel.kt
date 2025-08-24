package com.example.oppty1024.ai_edge_gallery.feature.chat.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oppty1024.ai_edge_gallery.feature.chat.data.*
import com.example.oppty1024.ai_edge_gallery.feature.chat.model.ChatModelHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChatViewModel"

/**
 * 聊天界面的UI状态
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isInProgress: Boolean = false,
    val isPreparing: Boolean = false,
    val isResettingSession: Boolean = false,
    val errorMessage: String? = null,
    val currentModel: Model? = null
)

/**
 * 聊天界面的ViewModel
 */
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /**
     * 设置当前模型
     */
    fun setCurrentModel(model: Model) {
        _uiState.value = _uiState.value.copy(currentModel = model)
    }

    /**
     * 初始化模型
     */
    fun initializeModel(context: Context, model: Model) {
        viewModelScope.launch(Dispatchers.IO) {
            model.initializing = true
            ChatModelHelper.initialize(context, model) { error ->
                model.initializing = false
                if (error.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(errorMessage = error)
                }
            }
        }
    }

    /**
     * 添加消息
     */
    fun addMessage(message: ChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
    }

    /**
     * 移除最后一条消息
     */
    fun removeLastMessage() {
        val currentMessages = _uiState.value.messages.toMutableList()
        if (currentMessages.isNotEmpty()) {
            currentMessages.removeLastOrNull()
            _uiState.value = _uiState.value.copy(messages = currentMessages)
        }
    }

    /**
     * 获取最后一条消息
     */
    fun getLastMessage(): ChatMessage? {
        return _uiState.value.messages.lastOrNull()
    }

    /**
     * 更新最后一条文本消息的内容
     */
    fun updateLastTextMessageContent(partialContent: String) {
        val currentMessages = _uiState.value.messages.toMutableList()
        if (currentMessages.isNotEmpty()) {
            val lastMessage = currentMessages.last()
            if (lastMessage is ChatMessageText) {
                currentMessages[currentMessages.size - 1] = lastMessage.copy(
                    content = partialContent,
                    isStreaming = true
                )
                _uiState.value = _uiState.value.copy(messages = currentMessages)
            }
        }
    }

    /**
     * 完成最后一条文本消息的流式更新
     */
    fun finishLastTextMessage() {
        val currentMessages = _uiState.value.messages.toMutableList()
        if (currentMessages.isNotEmpty()) {
            val lastMessage = currentMessages.last()
            if (lastMessage is ChatMessageText) {
                currentMessages[currentMessages.size - 1] = lastMessage.copy(isStreaming = false)
                _uiState.value = _uiState.value.copy(messages = currentMessages)
            }
        }
    }

    /**
     * 清除所有消息
     */
    fun clearAllMessages() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    /**
     * 设置进行中状态
     */
    fun setInProgress(inProgress: Boolean) {
        _uiState.value = _uiState.value.copy(isInProgress = inProgress)
    }

    /**
     * 设置准备中状态
     */
    fun setPreparing(preparing: Boolean) {
        _uiState.value = _uiState.value.copy(isPreparing = preparing)
    }

    /**
     * 设置重置会话状态
     */
    fun setIsResettingSession(resetting: Boolean) {
        _uiState.value = _uiState.value.copy(isResettingSession = resetting)
    }

    /**
     * 生成响应
     */
    fun generateResponse(
        model: Model,
        input: String,
        images: List<Bitmap> = listOf(),
        onError: () -> Unit
    ) {
        val accelerator = model.getStringConfigValue(key = ConfigKeys.ACCELERATOR, defaultValue = "")
        
        viewModelScope.launch(Dispatchers.Default) {
            try {
                setInProgress(true)
                setPreparing(true)

                // 添加加载消息
                addMessage(ChatMessageLoading(accelerator = accelerator))

                // 等待模型实例初始化完成
                while (model.instance == null && !model.initializing) {
                    delay(100)
                }
                
                delay(500) // 稍微延迟以显示加载状态

                // 运行推理
                val instance = model.instance as? LlmModelInstance
                if (instance == null) {
                    onError()
                    return@launch
                }

                var firstRun = true
                val start = System.currentTimeMillis()

                ChatModelHelper.runInference(
                    model = model,
                    input = input,
                    images = images,
                    resultListener = { partialResult, done ->
                        if (firstRun) {
                            // 移除加载消息，添加空的响应消息
                            val lastMessage = getLastMessage()
                            if (lastMessage is ChatMessageLoading) {
                                removeLastMessage()
                                addMessage(
                                    ChatMessageText(
                                        content = "",
                                        side = ChatSide.AGENT,
                                        accelerator = accelerator
                                    )
                                )
                            }
                            firstRun = false
                            setPreparing(false)
                        }

                        // 增量更新响应内容
                        updateLastTextMessageContent(partialResult)

                        if (done) {
                            setInProgress(false)
                            finishLastTextMessage()
                            Log.d(TAG, "推理完成，用时: ${System.currentTimeMillis() - start}ms")
                        }
                    },
                    cleanUpListener = {
                        setInProgress(false)
                        setPreparing(false)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "生成响应时发生错误", e)
                setInProgress(false)
                setPreparing(false)
                onError()
            }
        }
    }

    /**
     * 停止响应生成
     */
    fun stopResponse(model: Model) {
        Log.d(TAG, "停止响应生成...")
        
        // 移除加载消息
        val lastMessage = getLastMessage()
        if (lastMessage is ChatMessageLoading) {
            removeLastMessage()
        }
        
        viewModelScope.launch(Dispatchers.Default) {
            setInProgress(false)
            ChatModelHelper.stopInference(model)
        }
    }

    /**
     * 重置会话
     */
    fun resetSession(model: Model) {
        viewModelScope.launch(Dispatchers.Default) {
            setIsResettingSession(true)
            clearAllMessages()
            stopResponse(model)

            while (true) {
                try {
                    ChatModelHelper.resetSession(model)
                    break
                } catch (e: Exception) {
                    Log.d(TAG, "重置会话失败，重试中...")
                }
                delay(200)
            }
            
            setIsResettingSession(false)
        }
    }

    /**
     * 重新运行
     */
    fun runAgain(model: Model, message: ChatMessageText, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            // 等待模型初始化完成
            while (model.instance == null) {
                delay(100)
            }

            // 克隆消息并添加
            addMessage(message.clone())

            // 运行推理
            generateResponse(model = model, input = message.content, onError = onError)
        }
    }

    /**
     * 处理错误
     */
    fun handleError(context: Context, model: Model, triggeredMessage: ChatMessageText?) {
        // 清理模型
        cleanupModel(context, model)

        // 移除加载消息
        val lastMessage = getLastMessage()
        if (lastMessage is ChatMessageLoading) {
            removeLastMessage()
        }

        // 移除触发错误的消息
        if (getLastMessage() == triggeredMessage) {
            removeLastMessage()
        }

        // 添加警告消息
        addMessage(
            ChatMessageWarning(content = "发生错误，正在重新初始化会话...")
        )

        // 重新添加触发消息
        if (triggeredMessage != null) {
            addMessage(triggeredMessage)
        }

        // 重新初始化模型
        initializeModel(context, model)

        // 自动重新生成响应
        if (triggeredMessage != null) {
            generateResponse(model = model, input = triggeredMessage.content, onError = {})
        }
    }

    /**
     * 清理模型
     */
    fun cleanupModel(context: Context, model: Model) {
        viewModelScope.launch(Dispatchers.IO) {
            ChatModelHelper.cleanUp(model) {}
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
