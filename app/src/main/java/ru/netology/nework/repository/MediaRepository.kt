package ru.netology.nework.repository

import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Media
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.FileUtils
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun upload(uri: Uri, type: AttachmentType): Media {
        return try {
            val file = FileUtils.getFileFromUri(uri) ?: throw IOException("Не удалось получить файл")

            val requestFile = file.asRequestBody(
                when (type) {
                    AttachmentType.IMAGE -> "image/*".toMediaType()
                    AttachmentType.VIDEO -> "video/*".toMediaType()
                    AttachmentType.AUDIO -> "audio/*".toMediaType()
                }
            )

            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.upload(body)
            if (!response.isSuccessful) {
                throw AppError.fromThrowable(
                    retrofit2.HttpException(response)
                )
            }

            response.body() ?: throw Exception("Empty response")
        } catch (e: Exception) {
            throw AppError.fromThrowable(e)
        }
    }
}