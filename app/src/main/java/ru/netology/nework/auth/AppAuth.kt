package ru.netology.nework.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nework.dto.Token

class AppAuth {
    private val _authStateFlow = MutableStateFlow<AuthState>(AuthState())
    val authStateFlow = _authStateFlow.asStateFlow()

    // Для удобства работы в UI
    val authState: LiveData<AuthState> = authStateFlow.asLiveData()

    @Synchronized
    fun setAuth(token: Token?) {
        _authStateFlow.value = AuthState(
            id = token?.id ?: 0,
            token = token?.token
        )
        token?.let { saveToken(it) } ?: clearToken()
    }

    // Для быстрого доступа из интерцепторов
    val token: String?
        get() = _authStateFlow.value.token

    data class AuthState(val id: Long = 0, val token: String? = null)

    private fun saveToken(token: Token) {
        // TODO: сохранить в SharedPreferences/DataStore
    }

    private fun clearToken() {
        // TODO: очистить токен
    }

    init {
        // TODO: загрузить токен при инициализации
    }
}