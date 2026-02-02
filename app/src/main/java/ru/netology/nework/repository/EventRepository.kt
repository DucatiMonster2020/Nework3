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

    suspend fun likeById(id: Long): Event {
        try {
            val response = apiService.likeEvent(id)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }
            return response.body() ?: throw Exception("Empty response")
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun dislikeById(id: Long): Event {
        try {
            val response = apiService.dislikeEvent(id)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }
            return response.body() ?: throw Exception("Empty response")
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun participate(id: Long): Event {
        try {
            val response = apiService.participateEvent(id)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }
            return response.body() ?: throw Exception("Empty response")
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun cancelParticipation(id: Long): Event {
        try {
            val response = apiService.cancelParticipation(id)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }
            return response.body() ?: throw Exception("Empty response")
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun save(event: Event): Event {
        try {
            val response = apiService.saveEvent(event)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }
            return response.body() ?: throw Exception("Empty response")
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun removeById(id: Long) {
        try {
            val response = apiService.deleteEvent(id)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }
}