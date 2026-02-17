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
import ru.netology.nework.error.AppError
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.utils.Constants.ERROR_LOAD_POST
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _mentionedUsers = MutableLiveData<List<User>>(emptyList())
    val mentionedUsers: LiveData<List<User>> = _mentionedUsers

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val response = apiService.getPostById(postId)
                if (response.isSuccessful) {
                    val post = response.body()
                    _post.value = post
                    post?.mentionIds?.let { loadMentionedUsers(it) }
                } else {
                    _error.value = AppError.ApiError(response.code(), ERROR_LOAD_POST)
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun loadMentionedUsers(userIds: List<Long>) {
        viewModelScope.launch {
            try {
                val users = userRepository.getUsersByIds(userIds)
                _mentionedUsers.value = users
            } catch (e: Exception) {
            }
        }
    }

    fun likePost(postId: Long) {
        viewModelScope.launch {
            try {
                val response = postRepository.likeById(postId)
                if (response != null) {
                    _post.value = response
                    response.mentionIds?.let { loadMentionedUsers(it) }
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                postRepository.removeById(postId)
                _post.value = null
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }
}