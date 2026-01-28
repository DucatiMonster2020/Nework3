package ru.netology.nework.model

sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    object Refreshing : LoadingState()
    data class Error(val message: String?) : LoadingState()
    object Success : LoadingState()
}