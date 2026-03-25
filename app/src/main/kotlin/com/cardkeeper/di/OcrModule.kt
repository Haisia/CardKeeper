package com.cardkeeper.di

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Singleton
    @Provides
    fun provideTextRecognizer(): TextRecognizer =
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
}
