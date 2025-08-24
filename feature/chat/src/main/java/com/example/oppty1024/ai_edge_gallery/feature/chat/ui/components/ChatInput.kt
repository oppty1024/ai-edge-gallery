package com.example.oppty1024.ai_edge_gallery.feature.chat.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.clickable

/**
 * 聊天输入组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendMessage: (String, List<Bitmap>) -> Unit,
    onStopGeneration: () -> Unit,
    isGenerating: Boolean = false,
    showStopButton: Boolean = false,
    enableImageInput: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val context = LocalContext.current
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val bitmaps = mutableListOf<Bitmap>()
        uris.forEach { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                        bitmaps.add(bitmap)
                    }
                }
            } catch (e: Exception) {
                // 处理图片加载错误
            }
        }
        selectedImages = bitmaps
    }

    Column(modifier = modifier) {
        // 显示选中的图片
        if (selectedImages.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "选中的图片 (${selectedImages.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedImages) { bitmap ->
                            Box {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "选中的图片",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // 删除按钮
                                IconButton(
                                    onClick = {
                                        selectedImages = selectedImages.filter { it != bitmap }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除图片",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 输入框和按钮
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 图片选择按钮
                if (enableImageInput) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isGenerating
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "选择图片",
                            tint = if (selectedImages.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // 文本输入框
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    enabled = !isGenerating,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank() || selectedImages.isNotEmpty()) {
                                onSendMessage(text, selectedImages)
                                selectedImages = emptyList()
                            }
                        }
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 发送/停止按钮
                if (showStopButton && isGenerating) {
                    IconButton(
                        onClick = onStopGeneration,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "停止生成"
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (text.isNotBlank() || selectedImages.isNotEmpty()) {
                                onSendMessage(text, selectedImages)
                                selectedImages = emptyList()
                            }
                        },
                        enabled = !isGenerating && (text.isNotBlank() || selectedImages.isNotEmpty()),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送消息"
                        )
                    }
                }
            }
        }
    }
}
