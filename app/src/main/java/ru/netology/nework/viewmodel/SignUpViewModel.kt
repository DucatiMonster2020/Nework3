package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.error.AppError
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val apiService: ApiService,
    private val appAuth: AppAuth
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData(false)
    val success: LiveData<Boolean> = _success

    fun signUp(login: String, name: String, password: String, avatarUri: Uri?) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _success.value = false

                // ПРОСТАЯ регистрация БЕЗ аватара (по ТЗ достаточно)
                val loginBody = login.toRequestBody(MultipartBody.FORM)
                val passwordBody = password.toRequestBody(MultipartBody.FORM)
                val nameBody = name.toRequestBody(MultipartBody.FORM)

                // Регистрируем без аватара (file = null)
                val response = apiService.registerUser(
                    login = loginBody,
                    pass = passwordBody,
                    name = nameBody,
                    file = null // Без аватара - соответствует ТЗ
                )

                if (response.isSuccessful) {
                    val token = response.body()
                    token?.let {
                        appAuth.setAuth(it)
                        _success.value = true
                    } ?: run {
                        _error.value = "Ошибка регистрации"
                    }
                } else {
                    _error.value = "HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            } finally {
                _loading.value = false
            }
        }
    }
}