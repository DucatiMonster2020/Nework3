package ru.netology.nework.api


import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.nework.BuildConfig

fun loggingInterceptor() = HttpLoggingInterceptor().apply {
    if (BuildConfig.DEBUG) {
        level = HttpLoggingInterceptor.Level.BODY
    }
}