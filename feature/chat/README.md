# Chat 模块

基于 Google AI Edge Gallery 的 LLMChat 模块实现的聊天功能模块。

## 功能特性

- ✅ 支持本地 LLM 模型推理
- ✅ 流式响应显示
- ✅ 图片输入支持
- ✅ 模型动态选择和配置
- ✅ 会话管理（重置、重新生成）
- ✅ 多种加速器支持（CPU/GPU）
- ✅ 现代化 Material 3 UI 设计

## 架构组件

### 数据层
- `ChatMessage`: 聊天消息基础数据类
- `Model`: LLM 模型配置和状态管理
- `ConfigKeys` & `DefaultValues`: 模型配置常量

### 业务逻辑层  
- `ChatModelHelper`: 模型初始化和推理管理
- `ModelManager`: 模型发现和选择管理
- `ChatViewModel`: 聊天界面状态管理

### UI 层
- `ChatScreen`: 主聊天界面
- `ChatMessageItem`: 消息项组件
- `ChatInput`: 输入组件
- `ModelSelectionDialog`: 模型选择对话框

### 依赖注入
- `ChatModule`: Hilt 依赖注入配置

## 使用方法

### 1. 集成到主应用

在主应用的 `build.gradle.kts` 中添加依赖：

```kotlin
implementation(project(":feature:chat"))
```

### 2. 添加到导航

在 `MainActivity` 或导航组件中添加：

```kotlin
import com.example.oppty1024.ai_edge_gallery.feature.chat.ui.ChatScreen

// 在 Compose 导航中
composable("chat") {
    ChatScreen(
        navigateUp = { navController.popBackStack() }
    )
}
```

### 3. 准备模型文件

将 LLM 模型文件放置在以下目录：
```
/Android/data/com.example.oppty1024.ai_edge_gallery/files/models/
```

支持的模型格式：
- `.bin` (MediaPipe 格式)
- `.tflite` (TensorFlow Lite)
- `.gguf` (GGML 格式)

### 4. 推荐模型

以下是一些适用于移动设备的轻量级模型：

- **Gemma 2B**: `gemma-2b-it-cpu-int4.bin`
- **Phi-3 Mini**: `phi-3-mini-4k-instruct-cpu.bin`
- **Llama 3.2 1B**: `llama-3.2-1b-instruct-cpu.bin`

## 配置选项

### 模型配置
- **Max Tokens**: 最大生成token数 (128-4096)
- **Top-K**: 采样时保留的top-k候选 (1-100)
- **Top-P**: 核采样概率阈值 (0.1-1.0)
- **Temperature**: 生成随机性控制 (0.1-2.0)
- **Accelerator**: 加速器选择 (CPU/GPU)

### 默认配置
```kotlin
MAX_TOKEN = 1024
TOPK = 40
TOPP = 0.9f
TEMPERATURE = 1.0f
ACCELERATOR = GPU
```

## API 使用

### 直接使用 ChatScreen

```kotlin
@Composable
fun MyChatApp() {
    ChatScreen(
        navigateUp = { /* 返回逻辑 */ }
    )
}
```

### 使用自定义模型

```kotlin
val customModel = Model(
    name = "自定义模型",
    path = "models/my_model.bin",
    description = "我的自定义LLM模型",
    configs = mapOf(
        ConfigKeys.MAX_TOKENS to 2048,
        ConfigKeys.TEMPERATURE to 0.7f
    )
)

// 通过 ModelManager 添加模型
modelManager.addCustomModel(customModel)
```

## 注意事项

### 性能优化
1. 首次加载模型可能需要较长时间
2. 建议在GPU设备上使用GPU加速
3. 模型文件大小直接影响内存占用

### 内存管理
- 模型会在应用生命周期内保持加载状态
- 切换模型时会自动清理前一个模型
- 退出聊天界面时自动清理所有资源

### 兼容性
- 最低 Android API 26 (Android 8.0)
- 需要 MediaPipe Tasks GenAI 库支持
- 建议设备内存 ≥ 4GB

## 故障排除

### 常见问题

**1. 模型无法加载**
- 检查模型文件路径是否正确
- 确认模型格式是否支持
- 查看设备剩余内存是否充足

**2. 推理速度慢**
- 尝试切换到GPU加速器
- 减少Max Tokens设置
- 考虑使用更小的模型

**3. 应用崩溃**
- 检查设备内存是否足够
- 确认MediaPipe库版本兼容性
- 查看日志输出错误信息

### 日志标签
- `ChatModelHelper`: 模型操作相关
- `ChatViewModel`: 界面状态相关  
- `ModelManager`: 模型管理相关

## 扩展开发

### 添加新的消息类型

```kotlin
data class ChatMessageAudio(
    override val id: String = generateId(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val side: ChatSide,
    val audioData: ByteArray
) : ChatMessage()
```

### 自定义模型助手

```kotlin
object CustomModelHelper {
    fun initializeCustomModel(
        context: Context, 
        model: Model,
        onDone: (String) -> Unit
    ) {
        // 自定义初始化逻辑
    }
}
```

## 许可证

基于 Apache License 2.0，与原始 Google AI Edge Gallery 项目保持一致。
