package ru.netology.nework.repository

import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Event
import ru.netology.nework.error.AppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getAll(): List<Event> {
        try {
            val response = apiService.getAllEvents()
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }
            return response.body() ?: emptyList()
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    // ... остальные методы для событий
}