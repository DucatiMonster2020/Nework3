package ru.netology.nework.dto

data class Token(
    val id: Long,
    val token: String,
    val avatar: String? = null
)

data class PushToken(val token: String)