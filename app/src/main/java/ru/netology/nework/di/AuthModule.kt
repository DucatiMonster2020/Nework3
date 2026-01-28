package ru.netology.nework.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.auth.AppAuth
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AuthModule {

    @Singleton
    @Provides
    fun provideAppAuth(): AppAuth = AppAuth()
}