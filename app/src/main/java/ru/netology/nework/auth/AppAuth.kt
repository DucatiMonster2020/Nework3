package ru.netology.nework.auth

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nework.dto.Token

class AppAuth(private val context: Context) {
    private val _authStateFlow = MutableStateFlow<AuthState>(AuthState())
    val authStateFlow = _authStateFlow.asStateFlow()

    val authState: LiveData<AuthState> = authStateFlow.asLiveData()

    @Synchronized
    fun setAuth(token: Token?) {
        _authStateFlow.value = AuthState(
            id = token?.id ?: 0,
            token = token?.token
        )
        token?.let { saveToken(it) } ?: clearToken()
    }

    val token: String?
        get() = _authStateFlow.value.token

    data class AuthState(val id: Long = 0, val token: String? = null)

    private fun saveToken(token: Token) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit {
            putString("token", token.token)
            putLong("id", token.id)
            token.avatar?.let { putString("avatar", it) }
        }
    }

    private fun clearToken() {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit {
            clear()
        }
    }

    init {
        loadToken()
    }

    private fun loadToken() {
        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val id = prefs.getLong("id", 0L)
        val avatar = prefs.getString("avatar", null)

        if (token != null && id != 0L) {
            _authStateFlow.value = AuthState(id = id, token = token)
        }
    }
}