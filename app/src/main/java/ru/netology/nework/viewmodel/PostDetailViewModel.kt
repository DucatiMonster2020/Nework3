package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.utils.Constants
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _mentionedUsers = MutableLiveData<List<User>>(emptyList())
    val mentionedUsers: LiveData<List<User>> = _mentionedUsers

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError?>()
    val error: LiveData<AppError?> = _error

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val response = apiService.getPostById(postId)
                if (response.isSuccessful) {
                    val post = response.body()
                    _post.value = post
                    post?.mentionIds?.let { loadMentionedUsers(it) }
                } else {
                    _error.value = ApiError(Constants.ERROR_LOAD_POST)
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadMentionedUsers(userIds: List<Long>) {
        try {
            val response = apiService.getAllUsers()
            if (response.isSuccessful) {
                val allUsers = response.body() ?: emptyList()
                val mentioned = allUsers.filter { user -> userIds.contains(user.id) }
                _mentionedUsers.value = mentioned
            }
        } catch (e: Exception) {
        }
    }

    fun likePost(postId: Long) {
        viewModelScope.launch {
            try {
                val response = postRepository.likeById(postId)
                if (response != null) {
                    _post.value = response
                }
            } catch (e: Exception) {
                _error.value = ApiError(Constants.ERROR_LIKE)
            }
        }
    }
}