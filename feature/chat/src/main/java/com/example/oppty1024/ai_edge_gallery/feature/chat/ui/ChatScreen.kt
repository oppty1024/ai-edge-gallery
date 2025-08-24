package com.example.oppty1024.ai_edge_gallery.feature.chat.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.oppty1024.ai_edge_gallery.feature.chat.data.*
import com.example.oppty1024.ai_edge_gallery.feature.chat.repository.ModelManager
import com.example.oppty1024.ai_edge_gallery.feature.chat.ui.components.ChatInput
import com.example.oppty1024.ai_edge_gallery.feature.chat.ui.components.ChatMessageItem
import com.example.oppty1024.ai_edge_gallery.feature.chat.ui.components.ModelSelectionDialog
import com.example.oppty1024.ai_edge_gallery.feature.chat.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

/**
 * 聊天界面主屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navigateUp: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // 创建ModelManager实例
    val modelManager = remember { ModelManager() }
    
    val availableModels by modelManager.availableModels.collectAsStateWithLifecycle()
    val selectedModel by modelManager.selectedModel.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    var showModelSelection by remember { mutableStateOf(false) }
    
    // 初始化模型管理器
    LaunchedEffect(Unit) {
        modelManager.initialize(context)
    }
    
    // 当选中模型改变时，更新ChatViewModel
    LaunchedEffect(selectedModel) {
        selectedModel?.let { model ->
            viewModel.setCurrentModel(model)
            // 如果模型未初始化，则初始化模型
            if (model.instance == null && !model.initializing) {
                viewModel.initializeModel(context, model)
            }
        }
    }
    
    // 自动滚动到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    // 错误处理
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            // 可以在这里显示错误对话框或Snackbar
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AI聊天")
                        selectedModel?.let { model ->
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: run {
                            Text(
                                text = "选择模型开始聊天",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        IconButton(onClick = navigateUp) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                actions = {
                    // 模型选择按钮
                    IconButton(
                        onClick = { showModelSelection = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "选择模型"
                        )
                    }
                    
                    // 重置会话按钮
                    IconButton(
                        onClick = {
                            selectedModel?.let { model ->
                                viewModel.resetSession(model)
                            }
                        },
                        enabled = !uiState.isResettingSession && selectedModel != null
                    ) {
                        if (uiState.isResettingSession) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重置会话"
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 显示模型状态
            if (selectedModel?.initializing == true) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在初始化模型...")
                        }
                    }
                }
            } else if (selectedModel == null) {
                // 没有模型时的状态
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "请选择一个模型开始聊天",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 聊天消息列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SmartToy,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "开始与AI聊天吧！",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "输入消息或上传图片开始对话",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    items(uiState.messages) { message ->
                        ChatMessageItem(
                            message = message,
                            onRunAgainClick = if (message is ChatMessageText && message.side == ChatSide.AGENT) {
                                {
                                    selectedModel?.let { model ->
                                        viewModel.runAgain(
                                            model = model,
                                            message = message,
                                            onError = {
                                                viewModel.handleError(context, model, message)
                                            }
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                }

                // 聊天输入框
                ChatInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSendMessage = { text, images ->
                        selectedModel?.let { model ->
                            // 添加用户消息
                            if (text.isNotBlank()) {
                                viewModel.addMessage(
                                    ChatMessageText(
                                        content = text,
                                        side = ChatSide.USER
                                    )
                                )
                            }
                            
                            // 添加图片消息（如果有）
                            if (images.isNotEmpty()) {
                                viewModel.addMessage(
                                    ChatMessageImage(
                                        bitmaps = images,
                                        side = ChatSide.USER
                                    )
                                )
                            }
                            
                            // 生成响应
                            viewModel.generateResponse(
                                model = model,
                                input = text,
                                images = images,
                                onError = {
                                    val triggeredMessage = if (text.isNotBlank()) {
                                        ChatMessageText(content = text, side = ChatSide.USER)
                                    } else null
                                    viewModel.handleError(context, model, triggeredMessage)
                                }
                            )
                            
                            // 清空输入
                            inputText = ""
                        }
                    },
                    onStopGeneration = {
                        selectedModel?.let { model ->
                            viewModel.stopResponse(model)
                        }
                    },
                    isGenerating = uiState.isInProgress,
                    showStopButton = true,
                    enableImageInput = true // 根据模型能力启用图片输入
                )
            }
        }
    }
    
    // 模型选择对话框
    if (showModelSelection) {
        ModelSelectionDialog(
            models = availableModels,
            selectedModel = selectedModel,
            onModelSelected = { model ->
                modelManager.selectModel(model)
            },
            onDismiss = { showModelSelection = false }
        )
    }
}

// 预览组件
@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen()
    }
}
