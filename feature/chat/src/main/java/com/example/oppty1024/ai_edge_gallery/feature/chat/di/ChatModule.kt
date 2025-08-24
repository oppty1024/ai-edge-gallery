package com.example.oppty1024.ai_edge_gallery.feature.chat.di

import com.example.oppty1024.ai_edge_gallery.feature.chat.repository.ModelManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Chat模块的依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    /**
     * 提供模型管理器单例
     */
    @Provides
    @Singleton
    fun provideModelManager(): ModelManager {
        return ModelManager()
    }
}
