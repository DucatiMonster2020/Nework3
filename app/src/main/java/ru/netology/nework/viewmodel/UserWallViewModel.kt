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
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class UserWallViewModel @Inject constructor(
    private val apiService: ApiService,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _lastJob = MutableLiveData<String?>()
    val lastJob: LiveData<String?> = _lastJob

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    fun loadUserWall(userId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val userResponse = apiService.getUserById(userId)
                if (userResponse.isSuccessful) {
                    _user.value = userResponse.body()
                }

                val wallResponse = apiService.getUserWall(userId)
                if (wallResponse.isSuccessful) {
                    val wallPosts = wallResponse.body() ?: emptyList()
                    _posts.value = wallPosts
                    determineLastJob(wallPosts)
                } else {
                    _error.value = AppError.ApiError(
                        code = wallResponse.code(),
                        message = "Ошибка загрузки стены: ${wallResponse.code()}"
                    )
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshUserWall(userId: Long) {
        loadUserWall(userId)
    }

    private fun determineLastJob(posts: List<Post>) {
        val jobPost = posts.firstOrNull { !it.authorJob.isNullOrEmpty() }
        _lastJob.value = jobPost?.authorJob ?: "В поиске работы"
    }

    fun likeById(postId: Long) {
        viewModelScope.launch {
            try {
                val response = postRepository.likeById(postId)
                if (response != null) {
                    val currentPosts = _posts.value ?: emptyList()
                    val updatedPosts = currentPosts.map { post ->
                        if (post.id == postId) response else post
                    }
                    _posts.value = updatedPosts
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            try {
                postRepository.removeById(postId)
                val currentPosts = _posts.value ?: emptyList()
                val updatedPosts = currentPosts.filter { it.id != postId }
                _posts.value = updatedPosts
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }
}