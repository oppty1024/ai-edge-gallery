package com.example.oppty1024.ai_edge_gallery.feature.chat.repository

import android.content.Context
import android.util.Log
import com.example.oppty1024.ai_edge_gallery.feature.chat.data.*
import com.example.oppty1024.ai_edge_gallery.feature.chat.model.ChatModelHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModelManager"

/**
 * 模型管理器，负责管理和配置可用的AI模型
 */
@Singleton
class ModelManager @Inject constructor() {
    
    private val _availableModels = MutableStateFlow<List<Model>>(emptyList())
    val availableModels: StateFlow<List<Model>> = _availableModels.asStateFlow()
    
    private val _selectedModel = MutableStateFlow<Model?>(null)
    val selectedModel: StateFlow<Model?> = _selectedModel.asStateFlow()

    /**
     * 初始化模型管理器，扫描本地可用的模型
     */
    fun initialize(context: Context) {
        Log.d(TAG, "初始化模型管理器...")
        val models = scanLocalModels(context)
        _availableModels.value = models
        
        // 如果有可用模型且没有选中模型，默认选择第一个
        if (models.isNotEmpty() && _selectedModel.value == null) {
            _selectedModel.value = models.first()
        }
        
        Log.d(TAG, "发现 ${models.size} 个本地模型")
    }

    /**
     * 扫描本地模型文件
     */
    private fun scanLocalModels(context: Context): List<Model> {
        val models = mutableListOf<Model>()
        val externalFilesDir = context.getExternalFilesDir(null)
        
        if (externalFilesDir != null) {
            val modelsDir = File(externalFilesDir, "models")
            if (modelsDir.exists() && modelsDir.isDirectory) {
                Log.d(TAG, "扫描模型目录: ${modelsDir.absolutePath}")
                
                modelsDir.listFiles { file ->
                    file.isFile && (file.name.endsWith(".bin") || 
                                   file.name.endsWith(".tflite") || 
                                   file.name.endsWith(".gguf"))
                }?.forEach { modelFile ->
                    Log.d(TAG, "发现模型文件: ${modelFile.name}")
                    val model = createModelFromFile(modelFile)
                    models.add(model)
                }
            } else {
                Log.d(TAG, "模型目录不存在，创建默认模型配置")
                // 如果没有模型目录，创建一些示例模型配置
                models.addAll(createDefaultModels())
            }
        }
        
        return models
    }

    /**
     * 从文件创建模型对象
     */
    private fun createModelFromFile(file: File): Model {
        val fileName = file.nameWithoutExtension
        return Model(
            name = fileName.replace("_", " ").replaceFirstChar { it.uppercaseChar() },
            path = file.absolutePath,
            description = "本地模型文件: ${file.name}",
            configs = createDefaultConfigs()
        )
    }

    /**
     * 创建默认模型配置
     */
    private fun createDefaultModels(): List<Model> {
        return listOf(
            Model(
                name = "Gemma 2B",
                path = "models/gemma-2b-it-cpu-int4.bin",
                description = "Google Gemma 2B 指令调优模型 (CPU优化)",
                configs = createDefaultConfigs()
            ),
            Model(
                name = "Phi-3 Mini",
                path = "models/phi-3-mini-4k-instruct-cpu.bin", 
                description = "Microsoft Phi-3 Mini 4K 指令模型",
                configs = createDefaultConfigs()
            ),
            Model(
                name = "Llama 3.2 1B",
                path = "models/llama-3.2-1b-instruct-cpu.bin",
                description = "Meta Llama 3.2 1B 指令模型",
                configs = createDefaultConfigs()
            )
        )
    }

    /**
     * 创建默认配置
     */
    private fun createDefaultConfigs(): Map<String, Any> {
        return mapOf(
            ConfigKeys.MAX_TOKENS to DefaultValues.MAX_TOKEN,
            ConfigKeys.TOPK to DefaultValues.TOPK,
            ConfigKeys.TOPP to DefaultValues.TOPP,
            ConfigKeys.TEMPERATURE to DefaultValues.TEMPERATURE,
            ConfigKeys.ACCELERATOR to Accelerator.GPU.label
        )
    }

    /**
     * 选择模型
     */
    fun selectModel(model: Model) {
        Log.d(TAG, "选择模型: ${model.name}")
        _selectedModel.value = model
    }

    /**
     * 获取模型配置选项
     */
    fun getModelConfigOptions(): List<ModelConfigOption> {
        return listOf(
            ModelConfigOption(
                key = ConfigKeys.MAX_TOKENS,
                label = "最大Token数",
                type = ConfigType.INTEGER,
                defaultValue = DefaultValues.MAX_TOKEN,
                range = 128..4096
            ),
            ModelConfigOption(
                key = ConfigKeys.TOPK,
                label = "Top-K",
                type = ConfigType.INTEGER,
                defaultValue = DefaultValues.TOPK,
                range = 1..100
            ),
            ModelConfigOption(
                key = ConfigKeys.TOPP,
                label = "Top-P",
                type = ConfigType.FLOAT,
                defaultValue = DefaultValues.TOPP,
                range = 0.1f..1.0f
            ),
            ModelConfigOption(
                key = ConfigKeys.TEMPERATURE,
                label = "Temperature",
                type = ConfigType.FLOAT,
                defaultValue = DefaultValues.TEMPERATURE,
                range = 0.1f..2.0f
            ),
            ModelConfigOption(
                key = ConfigKeys.ACCELERATOR,
                label = "加速器",
                type = ConfigType.ENUM,
                defaultValue = Accelerator.GPU.label,
                options = listOf(Accelerator.CPU.label, Accelerator.GPU.label)
            )
        )
    }

    /**
     * 更新模型配置
     */
    fun updateModelConfig(model: Model, key: String, value: Any) {
        val updatedConfigs = model.configs.toMutableMap()
        updatedConfigs[key] = value
        
        val updatedModel = model.copy(configs = updatedConfigs)
        
        // 更新模型列表
        val updatedModels = _availableModels.value.map { 
            if (it.name == model.name) updatedModel else it 
        }
        _availableModels.value = updatedModels
        
        // 如果是当前选中的模型，也要更新
        if (_selectedModel.value?.name == model.name) {
            _selectedModel.value = updatedModel
        }
        
        Log.d(TAG, "更新模型 ${model.name} 的配置: $key = $value")
    }

    /**
     * 清理所有模型资源
     */
    fun cleanupAllModels() {
        Log.d(TAG, "清理所有模型资源...")
        _availableModels.value.forEach { model ->
            if (model.instance != null) {
                ChatModelHelper.cleanUp(model) {}
            }
        }
    }
}

/**
 * 模型配置选项
 */
data class ModelConfigOption(
    val key: String,
    val label: String,
    val type: ConfigType,
    val defaultValue: Any,
    val range: Any? = null,
    val options: List<String>? = null
)

/**
 * 配置类型
 */
enum class ConfigType {
    INTEGER,
    FLOAT,
    STRING,
    BOOLEAN,
    ENUM
}
