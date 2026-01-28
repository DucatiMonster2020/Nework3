package ru.netology.nework.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nework.dto.Token

object AuthStateManager {

    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthorized)
    val authState: LiveData<AuthState> = _authState

    private var currentToken: Token? = null

    init {
        // TODO: загрузить токен из SharedPreferences
    }

    fun signIn(token: Token) {
        currentToken = token
        _authState.value = AuthState.Authorized(token)
        // TODO: сохранить токен в SharedPreferences
    }

    fun signOut() {
        currentToken = null
        _authState.value = AuthState.Unauthorized
        // TODO: очистить токен из SharedPreferences
    }

    fun getToken(): String? = currentToken?.token

    sealed class AuthState {
        object Unauthorized : AuthState()
        data class Authorized(val token: Token) : AuthState()
    }
}