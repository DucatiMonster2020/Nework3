package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.error.AppError
import ru.netology.nework.model.FeedModel
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel> = _dataState

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            _dataState.value = _dataState.value?.copy(loading = true)
            try {
                val posts = repository.getAll()
                _dataState.value = FeedModel(
                    posts = posts,
                    empty = posts.isEmpty(),
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

    fun refreshPosts() {
        viewModelScope.launch {
            _dataState.value = _dataState.value?.copy(refreshing = true)
            try {
                val posts = repository.getAll()
                _dataState.value = FeedModel(
                    posts = posts,
                    empty = posts.isEmpty(),
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

    fun likeById(id: Long) {
        viewModelScope.launch {
            try {
                val post = repository.likeById(id)
                val currentPosts = _dataState.value?.posts ?: emptyList()
                val newPosts = currentPosts.map { if (it.id == id) post else it }
                _dataState.value = _dataState.value?.copy(posts = newPosts)
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                val currentPosts = _dataState.value?.posts ?: emptyList()
                val newPosts = currentPosts.filter { it.id != id }
                _dataState.value = _dataState.value?.copy(posts = newPosts)
                repository.removeById(id)
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
                loadPosts()
            }
        }
    }
}