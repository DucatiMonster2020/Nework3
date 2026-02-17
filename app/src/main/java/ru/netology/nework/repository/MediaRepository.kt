package ru.netology.nework.repository

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Media
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.AppError
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun upload(context: Context, uri: Uri, type: AttachmentType): Media {
        try {
            val filePart = createMediaPart(context, uri, type)
            val response = apiService.upload(filePart)

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

    private fun createMediaPart(context: Context, uri: Uri, type: AttachmentType): MultipartBody.Part {
        val mimeType = when (type) {
            AttachmentType.IMAGE -> "image/*"
            AttachmentType.VIDEO -> "video/*"
            AttachmentType.AUDIO -> "audio/*"
        }.toMediaType()

        val extension = when (type) {
            AttachmentType.IMAGE -> "jpg"
            AttachmentType.VIDEO -> "mp4"
            AttachmentType.AUDIO -> "mp3"
        }

        val tempFile = File.createTempFile("media_", ".$extension", context.cacheDir)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        val requestFile = tempFile.asRequestBody(mimeType)
        return MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
    }
}