package ru.netology.nework.repository

import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getAll(): List<Post> {
        try {
            val response = apiService.getAllPosts()
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

    suspend fun likeById(id: Long): Post {
        try {
            val response = apiService.likePost(id)
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

    suspend fun dislikeById(id: Long): Post {
        try {
            val response = apiService.dislikePost(id)
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

    suspend fun save(post: Post): Post {
        try {
            val response = apiService.savePost(post)
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
            val response = apiService.deletePost(id)
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