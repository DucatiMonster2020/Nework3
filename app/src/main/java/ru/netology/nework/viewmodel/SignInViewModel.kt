package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val apiService: ApiService,
    private val appAuth: AppAuth
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError?>()
    val error: LiveData<AppError?> = _error

    private val _success = SingleLiveEvent<Boolean>()
    val success: LiveData<Boolean> = _success

    fun signIn(login: String, password: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _success.value = false

                val response = apiService.authenticateUser(login, password)

                if (response.isSuccessful) {
                    val token = response.body()
                    token?.let {
                        appAuth.setAuth(it)
                        _success.value = true
                    } ?: run {
                        _error.value = ApiError("Ошибка авторизации")
                    }
                } else {
                    when (response.code()) {
                        400 -> _error.value = ApiError("Неправильный логин или пароль")
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
}