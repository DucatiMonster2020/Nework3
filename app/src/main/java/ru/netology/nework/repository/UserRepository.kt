package ru.netology.nework.repository

import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getAll(): List<User> {
        return apiService.getAllUsers().body() ?: emptyList()
    }
}