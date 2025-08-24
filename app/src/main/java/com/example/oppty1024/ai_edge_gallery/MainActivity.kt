package com.example.oppty1024.ai_edge_gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.oppty1024.ai_edge_gallery.navigation.BottomNavItem
import com.example.oppty1024.ai_edge_gallery.ui.components.BottomNavigationBar
import com.example.oppty1024.ai_edge_gallery.feature.chat.ui.ChatScreen
import com.example.oppty1024.ai_edge_gallery.ui.screens.MoreScreen
import com.example.oppty1024.ai_edge_gallery.ui.screens.TodoScreen
import com.example.oppty1024.ai_edge_gallery.ui.theme.AIEdgeGalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIEdgeGalleryTheme {
                AIEdgeGalleryApp()
            }
        }
    }
}

@Composable
fun AIEdgeGalleryApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Chat.route) {
                ChatScreen()
            }
            composable(BottomNavItem.Todo.route) {
                TodoScreen()
            }
            composable(BottomNavItem.More.route) {
                MoreScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AIEdgeGalleryAppPreview() {
    AIEdgeGalleryTheme {
        AIEdgeGalleryApp()
    }
}
