package com.cardkeeper.di

import android.content.Context
import com.cardkeeper.data.datasource.ImageStorageDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Singleton
    @Provides
    fun provideImageStorageDataSource(
        @ApplicationContext context: Context
    ): ImageStorageDataSource = ImageStorageDataSource(context)
}
