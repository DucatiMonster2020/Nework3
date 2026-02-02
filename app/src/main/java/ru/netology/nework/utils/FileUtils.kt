package ru.netology.nework.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtils {

    private const val MAX_FILE_SIZE_BYTES = 15 * 1024 * 1024 // 15 МБ

    fun isFileSizeValid(fileUri: Uri, context: Context): Boolean {
        return try {
            val fileSize = getFileSize(fileUri, context)
            fileSize <= MAX_FILE_SIZE_BYTES
        } catch (e: Exception) {
            false
        }
    }

    fun getFileSize(fileUri: Uri, context: Context): Long {
        return context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex)
        } ?: 0L
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f МБ", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f КБ", bytes / 1024.0)
            else -> "$bytes Б"
        }
    }

    fun getFileFromUri(uri: Uri): File? {
        return try {
            // Для файловой схемы
            if (uri.scheme == "file") {
                return File(uri.path ?: return null)
            }

            null // В реальном проекте нужна реализация копирования файла
        } catch (e: Exception) {
            null
        }
    }

    fun isImageFile(uri: Uri, context: Context): Boolean {
        val mimeType = context.contentResolver.getType(uri) ?: return false
        return mimeType.startsWith("image/")
    }

    fun isVideoFile(uri: Uri, context: Context): Boolean {
        val mimeType = context.contentResolver.getType(uri) ?: return false
        return mimeType.startsWith("video/")
    }
    fun isAudioFile(uri: Uri, context: Context): Boolean {
        val mimeType = context.contentResolver.getType(uri) ?: return false
        return mimeType.startsWith("audio/")
    }
}