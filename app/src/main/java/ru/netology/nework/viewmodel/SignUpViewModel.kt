package ru.netology.nework.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants
import ru.netology.nework.utils.Constants.MAX_FILE_SIZE
import ru.netology.nework.utils.Constants.MAX_IMAGE_SIZE
import ru.netology.nework.utils.SingleLiveEvent
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val apiService: ApiService,
    private val appAuth: AppAuth
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError?>()
    val error: LiveData<AppError?> = _error

    private val _success = SingleLiveEvent<Boolean>()
    val success: LiveData<Boolean> = _success

    fun signUp(
        context: Context,
        login: String,
        name: String,
        password: String,
        avatarUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _success.value = false

                if (avatarUri == null) {
                    _error.value = ApiError("Аватар обязателен")
                    return@launch
                }

                val filePart = withContext(Dispatchers.IO) {
                    createAvatarPart(context, avatarUri)
                }

                if (filePart == null) {
                    _error.value = ApiError("Неверный формат или размер аватара")
                    return@launch
                }

                val loginBody = login.toRequestBody(MultipartBody.FORM)
                val passwordBody = password.toRequestBody(MultipartBody.FORM)
                val nameBody = name.toRequestBody(MultipartBody.FORM)

                val response = apiService.registerUser(
                    login = loginBody,
                    pass = passwordBody,
                    name = nameBody,
                    file = filePart
                )

                if (response.isSuccessful) {
                    val token = response.body()
                    token?.let {
                        appAuth.setAuth(it)
                        _success.value = true
                    } ?: run {
                        _error.value = ApiError("Ошибка регистрации")
                    }
                } else {
                    when (response.code()) {
                        400 -> _error.value = ApiError(Constants.ERROR_USER_ALREADY_EXISTS)
                        else -> _error.value = ApiError("Ошибка: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun createAvatarPart(context: Context, uri: Uri): MultipartBody.Part? {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            val isValidFormat = mimeType in arrayOf("image/jpeg", "image/png", "image/jpg")
            if (!isValidFormat) {
                return null
            }

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val width = options.outWidth
            val height = options.outHeight

            if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
                return null
            }

            val fileSize = getFileSize(context, uri)
            if (fileSize > MAX_FILE_SIZE) {
                return null
            }

            val tempFile = File.createTempFile("avatar_", ".${getExtension(mimeType)}", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val mediaType = when (mimeType) {
                "image/jpeg", "image/jpg" -> "image/jpeg"
                "image/png" -> "image/png"
                else -> "image/*"
            }.toMediaType()

            val requestFile = tempFile.asRequestBody(mediaType)
            MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

        } catch (e: Exception) {
            null
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                fd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getExtension(mimeType: String?): String {
        return when (mimeType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            else -> "jpg"
        }
    }
}
