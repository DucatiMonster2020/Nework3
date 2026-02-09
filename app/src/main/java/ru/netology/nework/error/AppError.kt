package ru.netology.nework.error

import java.io.IOException

sealed class AppError : RuntimeException() {
    companion object {
        fun fromThrowable(throwable: Throwable): AppError = when (throwable) {
            is IOException -> NetworkError()
            is retrofit2.HttpException -> {
                when (throwable.code()) {
                    400 -> ApiError(throwable.message())
                    401 -> AuthError()
                    403 -> AuthError()
                    404 -> NotFoundError()
                    409 -> ConflictError()
                    422 -> ValidationError()
                    500 -> ServerError()
                    else -> UnknownError()
                }
            }
            else -> UnknownError()
        }
    }
}

class ApiError(override val message: String?) : AppError()
class NetworkError : AppError()
class AuthError : AppError()
class NotFoundError : AppError()
class ConflictError : AppError()
class ValidationError : AppError()
class ServerError : AppError()
class UnknownError : AppError()