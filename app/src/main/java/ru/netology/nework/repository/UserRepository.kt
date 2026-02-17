package ru.netology.nework.repository

import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.User
import ru.netology.nework.error.AppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {

    private var userCache: Map<Long, User> = emptyMap()

    suspend fun getAll(): List<User> {
        try {
            val response = apiService.getAllUsers()
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    HttpException(response)
                )
            }
            val users = response.body() ?: emptyList()
            userCache = users.associateBy { it.id }
            return users
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun getUserById(id: Long): User? {
        userCache[id]?.let { return it }
        try {
            val response = apiService.getUserById(id)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    HttpException(response)
                )
            }
            return response.body()
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun getUsersByIds(ids: List<Long>): List<User> {
        if (ids.isEmpty()) return emptyList()
        if (userCache.isEmpty()) {
            getAll()
        }
        return ids.mapNotNull { userCache[it] }
    }
}