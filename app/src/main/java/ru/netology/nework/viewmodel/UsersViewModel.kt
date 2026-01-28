package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.dto.User
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _state = MutableLiveData<FeedModelState>(FeedModelState.IDLE)
    val state: LiveData<FeedModelState> = _state

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState.LOADING
                val users = repository.getAll()
                _users.value = users
                _state.value = FeedModelState.IDLE
            } catch (e: Exception) {
                _state.value = FeedModelState.error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshUsers() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState.REFRESHING
                val users = repository.getAll()
                _users.value = users
                _state.value = FeedModelState.IDLE
            } catch (e: Exception) {
                _state.value = FeedModelState.error(e.message ?: "Unknown error")
            }
        }
    }
}