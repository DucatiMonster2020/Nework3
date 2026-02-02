package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.model.FeedModel
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.PostRepository
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel> = _dataState

    private val _state = MutableLiveData<FeedModelState>(FeedModelState.IDLE)
    val state: LiveData<FeedModelState> = _state

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState.LOADING
                val posts = repository.getAll()
                _dataState.value = FeedModel(
                    posts = posts,
                    empty = posts.isEmpty()
                )
                _state.value = FeedModelState.IDLE
            } catch (e: Exception) {
                _state.value = FeedModelState.error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState.REFRESHING
                val posts = repository.getAll()
                _dataState.value = FeedModel(
                    posts = posts,
                    empty = posts.isEmpty()
                )
                _state.value = FeedModelState.IDLE
            } catch (e: Exception) {
                _state.value = FeedModelState.error(e.message ?: "Unknown error")
            }
        }
    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            try {
                val currentPosts = _dataState.value?.posts ?: emptyList()
                val post = currentPosts.find { it.id == id }
                post?.let { originalPost ->
                    val updatedPost = originalPost.copy(
                        likedByMe = !originalPost.likedByMe,
                        likeOwnerIds = if (originalPost.likedByMe) {
                            originalPost.likeOwnerIds - 999 // ваш ID
                        } else {
                            originalPost.likeOwnerIds + 999
                        }
                    )

                    val newPosts = currentPosts.map {
                        if (it.id == id) updatedPost else it
                    }

                    _dataState.value = _dataState.value?.copy(posts = newPosts)
                    if (updatedPost.likedByMe) {
                        repository.likeById(id)
                    } else {
                        repository.dislikeById(id)
                    }
                }
            } catch (e: Exception) {
                _state.value = FeedModelState.error("Failed to like post")
                loadPosts()
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
                _state.value = FeedModelState.error("Failed to delete post")
                loadPosts()
            }
        }
    }
}