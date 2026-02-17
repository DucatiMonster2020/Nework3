package ru.netology.nework.error

import java.io.IOException

sealed class AppError : RuntimeException() {

    class ApiError(val code: Int? = null, override val message: String?) : AppError()

    class NetworkError(override val message: String? = "Ошибка сети") : AppError()

    class AuthError(override val message: String? = "Ошибка авторизации") : AppError()

    class NotFoundError(override val message: String? = "Ресурс не найден") : AppError()

    class ConflictError(override val message: String? = "Конфликт данных") : AppError()

    class ValidationError(override val message: String? = "Ошибка валидации") : AppError()

    class ServerError(override val message: String? = "Ошибка сервера") : AppError()

    class UnknownError(override val message: String? = "Неизвестная ошибка") : AppError()

    companion object {
        fun fromThrowable(throwable: Throwable): AppError = when (throwable) {
            is IOException -> NetworkError(throwable.message)
            is retrofit2.HttpException -> {
                when (throwable.code()) {
                    400 -> ValidationError(throwable.message())
                    401 -> AuthError(throwable.message())
                    403 -> AuthError(throwable.message())
                    404 -> NotFoundError(throwable.message())
                    409 -> ConflictError(throwable.message())
                    422 -> ValidationError(throwable.message())
                    in 500..599 -> ServerError(throwable.message())
                    else -> ApiError(throwable.code(), throwable.message())
                }
            }
            else -> UnknownError(throwable.message)
        }
    }
}