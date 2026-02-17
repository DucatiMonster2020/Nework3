package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.error.AppError
import ru.netology.nework.model.FeedModel
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel> = _dataState

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _dataState.value = _dataState.value?.copy(loading = true)
            try {
                val users = repository.getAll()
                _dataState.value = FeedModel(
                    users = users,
                    empty = users.isEmpty(),
                    loading = false
                )
            } catch (e: Exception) {
                val error = AppError.fromThrowable(e)
                _dataState.value = _dataState.value?.copy(
                    loading = false,
                    error = error
                )
                _error.value = error
            }
        }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            _dataState.value = _dataState.value?.copy(refreshing = true)
            try {
                val users = repository.getAll()
                _dataState.value = FeedModel(
                    users = users,
                    empty = users.isEmpty(),
                    refreshing = false
                )
            } catch (e: Exception) {
                val error = AppError.fromThrowable(e)
                _dataState.value = _dataState.value?.copy(
                    refreshing = false,
                    error = error
                )
                _error.value = error
            }
        }
    }
}