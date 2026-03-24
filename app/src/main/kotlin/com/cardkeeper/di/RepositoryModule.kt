package com.cardkeeper.di

import com.cardkeeper.data.repository.CardRepositoryImpl
import com.cardkeeper.data.repository.TagRepositoryImpl
import com.cardkeeper.domain.repository.CardRepository
import com.cardkeeper.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Singleton
    @Binds
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
