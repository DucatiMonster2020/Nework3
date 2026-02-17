package ru.netology.nework.repository

import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Job
import ru.netology.nework.error.AppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun getMyJobs(): List<Job> {
        return try {
            val response = apiService.getMyJobs()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                throw AppError.fromThrowable(HttpException(response))
            }
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun getUserJobs(userId: Long): List<Job> {
        return try {
            val response = apiService.getUserJobs(userId)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                throw AppError.fromThrowable(HttpException(response))
            }
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun saveJob(job: Job): Job {
        return try {
            val response = apiService.saveJob(job)
            if (response.isSuccessful) {
                response.body() ?: throw AppError.ApiError(null, "Пустой ответ от сервера")
            } else {
                throw AppError.fromThrowable(HttpException(response))
            }
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }

    suspend fun deleteJob(jobId: Long) {
        try {
            val response = apiService.deleteJob(jobId)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(HttpException(response))
            }
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }
}