package com.example.oppty1024.ai_edge_gallery.feature.chat.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.oppty1024.ai_edge_gallery.feature.chat.data.*
// import com.google.mediapipe.framework.image.BitmapImageBuilder // 暂时注释，等MediaPipe版本支持
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

private const val TAG = "ChatModelHelper"

typealias ResultListener = (partialResult: String, done: Boolean) -> Unit
typealias CleanUpListener = () -> Unit

/**
 * 聊天模型助手类，负责管理LLM模型的初始化、推理等操作
 */
object ChatModelHelper {
    // 清理监听器映射，按模型名称索引
    private val cleanUpListeners: MutableMap<String, CleanUpListener> = mutableMapOf()

    /**
     * 初始化模型
     */
    fun initialize(context: Context, model: Model, onDone: (String) -> Unit) {
        Log.d(TAG, "初始化模型 '${model.name}'...")
        
        try {
            // 准备配置参数
            val maxTokens = model.getIntConfigValue(
                key = ConfigKeys.MAX_TOKENS, 
                defaultValue = DefaultValues.MAX_TOKEN
            )
            val topK = model.getIntConfigValue(
                key = ConfigKeys.TOPK, 
                defaultValue = DefaultValues.TOPK
            )
            val topP = model.getFloatConfigValue(
                key = ConfigKeys.TOPP, 
                defaultValue = DefaultValues.TOPP
            )
            val temperature = model.getFloatConfigValue(
                key = ConfigKeys.TEMPERATURE, 
                defaultValue = DefaultValues.TEMPERATURE
            )
            val accelerator = model.getStringConfigValue(
                key = ConfigKeys.ACCELERATOR, 
                defaultValue = Accelerator.GPU.label
            )

            Log.d(TAG, "配置参数: maxTokens=$maxTokens, topK=$topK, topP=$topP, temperature=$temperature, accelerator=$accelerator")

            // 设置后端
            val preferredBackend = when (accelerator) {
                Accelerator.CPU.label -> LlmInference.Backend.CPU
                Accelerator.GPU.label -> LlmInference.Backend.GPU
                else -> LlmInference.Backend.GPU
            }

            // 创建LLM推理选项
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(model.getPath(context))
                .setMaxTokens(maxTokens)
                .setPreferredBackend(preferredBackend)
                .setMaxNumImages(DefaultValues.MAX_IMAGE_COUNT)
                .build()

            // 创建LLM推理实例
            val llmInference = LlmInference.createFromOptions(context, options)

            // 创建会话
            val session = LlmInferenceSession.createFromOptions(
                llmInference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(topK)
                    .setTopP(topP)
                    .setTemperature(temperature)
                    .build()
            )

            // 保存实例
            model.instance = LlmModelInstance(engine = llmInference, session = session)
            
            Log.d(TAG, "模型 '${model.name}' 初始化成功")
            onDone("")
        } catch (e: Exception) {
            Log.e(TAG, "模型 '${model.name}' 初始化失败", e)
            onDone("模型初始化失败: ${e.message}")
        }
    }

    /**
     * 重置会话
     */
    fun resetSession(model: Model) {
        try {
            Log.d(TAG, "重置模型 '${model.name}' 的会话...")

            val instance = model.instance as LlmModelInstance? ?: return
            val session = instance.session
            session.close()

            val inference = instance.engine
            val topK = model.getIntConfigValue(key = ConfigKeys.TOPK, defaultValue = DefaultValues.TOPK)
            val topP = model.getFloatConfigValue(key = ConfigKeys.TOPP, defaultValue = DefaultValues.TOPP)
            val temperature = model.getFloatConfigValue(
                key = ConfigKeys.TEMPERATURE, 
                defaultValue = DefaultValues.TEMPERATURE
            )

            val newSession = LlmInferenceSession.createFromOptions(
                inference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(topK)
                    .setTopP(topP)
                    .setTemperature(temperature)
                    .build()
            )
            
            instance.session = newSession
            Log.d(TAG, "会话重置完成")
        } catch (e: Exception) {
            Log.e(TAG, "重置会话失败", e)
        }
    }

    /**
     * 清理模型资源
     */
    fun cleanUp(model: Model, onDone: () -> Unit) {
        if (model.instance == null) {
            onDone()
            return
        }

        val instance = model.instance as LlmModelInstance

        try {
            instance.session.close()
        } catch (e: Exception) {
            Log.e(TAG, "关闭LLM推理会话失败: ${e.message}")
        }

        try {
            instance.engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "关闭LLM推理引擎失败: ${e.message}")
        }

        val onCleanUp = cleanUpListeners.remove(model.name)
        onCleanUp?.invoke()
        
        model.instance = null
        
        onDone()
        Log.d(TAG, "模型清理完成")
    }

    /**
     * 运行推理
     */
    fun runInference(
        model: Model,
        input: String,
        resultListener: ResultListener,
        cleanUpListener: CleanUpListener,
        images: List<Bitmap> = listOf()
    ) {
        val instance = model.instance as LlmModelInstance

        // 设置清理监听器
        if (!cleanUpListeners.containsKey(model.name)) {
            cleanUpListeners[model.name] = cleanUpListener
        }

        try {
            // 开始异步推理
            val session = instance.session
            
            // 添加文本查询
            if (input.trim().isNotEmpty()) {
                session.addQueryChunk(input)
            }
            
            // 添加图片 - 暂时禁用，等MediaPipe framework依赖解决
            // for (image in images) {
            //     session.addImage(BitmapImageBuilder(image).build())
            // }

            // 生成响应
            session.generateResponseAsync(resultListener)
        } catch (e: Exception) {
            Log.e(TAG, "推理过程中发生错误", e)
            resultListener("推理失败: ${e.message}", true)
        }
    }

    /**
     * 停止推理
     */
    fun stopInference(model: Model) {
        try {
            val instance = model.instance as? LlmModelInstance ?: return
            instance.session.cancelGenerateResponseAsync()
            Log.d(TAG, "已停止模型 '${model.name}' 的推理")
        } catch (e: Exception) {
            Log.e(TAG, "停止推理失败", e)
        }
    }
}
