package com.example.oppty1024.ai_edge_gallery.feature.chat.data

import android.content.Context
import java.io.File

/**
 * LLM模型数据类
 */
data class Model(
    val name: String,
    val path: String,
    val description: String = "",
    val configs: Map<String, Any> = mapOf(),
    var instance: Any? = null,
    var initializing: Boolean = false
) {
    /**
     * 获取模型文件路径
     */
    fun getPath(context: Context): String {
        return if (File(path).isAbsolute) {
            path
        } else {
            File(context.getExternalFilesDir(null), path).absolutePath
        }
    }

    /**
     * 获取整型配置值
     */
    fun getIntConfigValue(key: String, defaultValue: Int = 0): Int {
        return configs[key] as? Int ?: defaultValue
    }

    /**
     * 获取浮点型配置值
     */
    fun getFloatConfigValue(key: String, defaultValue: Float = 0f): Float {
        return configs[key] as? Float ?: defaultValue
    }

    /**
     * 获取字符串配置值
     */
    fun getStringConfigValue(key: String, defaultValue: String = ""): String {
        return configs[key] as? String ?: defaultValue
    }
}

/**
 * 加速器类型
 */
enum class Accelerator(val label: String) {
    CPU("cpu"),
    GPU("gpu")
}

/**
 * 配置键常量
 */
object ConfigKeys {
    const val MAX_TOKENS = "maxTokens"
    const val TOPK = "topK"
    const val TOPP = "topP"
    const val TEMPERATURE = "temperature"
    const val ACCELERATOR = "accelerator"
}

/**
 * 默认配置值
 */
object DefaultValues {
    const val MAX_TOKEN = 1024
    const val TOPK = 40
    const val TOPP = 0.9f
    const val TEMPERATURE = 1.0f
    const val MAX_IMAGE_COUNT = 8
}

/**
 * LLM模型实例包装类
 */
data class LlmModelInstance(
    val engine: com.google.mediapipe.tasks.genai.llminference.LlmInference,
    var session: com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
)
