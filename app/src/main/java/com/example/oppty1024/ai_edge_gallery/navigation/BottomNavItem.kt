package com.example.oppty1024.ai_edge_gallery.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项枚举类
 */
enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val contentDescription: String
) {
    Chat(
        route = "chat",
        icon = Icons.AutoMirrored.Filled.Chat,
        label = "Chat",
        contentDescription = "Chat tab"
    ),
    Todo(
        route = "todo",
        icon = Icons.AutoMirrored.Filled.List,
        label = "Todo",
        contentDescription = "Todo tab"
    ),
    More(
        route = "more",
        icon = Icons.Default.MoreHoriz,
        label = "More",
        contentDescription = "More tab"
    );

    companion object {
        /**
         * 获取所有底部导航项
         */
        fun getAllItems(): List<BottomNavItem> = values().toList()
        
        /**
         * 根据路由获取对应的导航项
         */
        fun fromRoute(route: String?): BottomNavItem? = 
            values().find { it.route == route }
    }
}
