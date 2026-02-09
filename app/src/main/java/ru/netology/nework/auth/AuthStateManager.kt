package ru.netology.nework.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nework.dto.Token

object AuthStateManager {

    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthorized)
    val authState: LiveData<AuthState> = _authState

    private var currentToken: Token? = null

    fun signIn(token: Token) {
        currentToken = token
        _authState.value = AuthState.Authorized(token)
    }

    fun signOut() {
        currentToken = null
        _authState.value = AuthState.Unauthorized
    }

    fun getToken(): String? = currentToken?.token

    sealed class AuthState {
        object Unauthorized : AuthState()
        data class Authorized(val token: Token) : AuthState()
    }
}