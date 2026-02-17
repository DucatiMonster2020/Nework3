package ru.netology.nework.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.AppError
import ru.netology.nework.repository.MediaRepository
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val apiService: ApiService,
    private val mediaRepository: MediaRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    private val _success = SingleLiveEvent<Boolean>()
    val success: LiveData<Boolean> = _success

    suspend fun loadUsersByIds(userIds: List<Long>): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                userRepository.getUsersByIds(userIds)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun loadPostForEditing(postId: Long): Post? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPostById(postId)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun uploadMedia(context: Context, uri: Uri, type: AttachmentType): String {
        return withContext(Dispatchers.IO) {
            try {
                _loading.postValue(true)
                val response = mediaRepository.upload(context, uri, type)
                response.url
            } catch (e: Exception) {
                throw e
            } finally {
                _loading.postValue(false)
            }
        }
    }

    fun savePost(
        content: String,
        link: String? = null,
        coords: Coordinates? = null,
        mentionIds: List<Long> = emptyList(),
        attachment: Attachment? = null
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val post = Post(
                    id = 0,
                    author = "",
                    authorId = 0,
                    authorJob = null,
                    authorAvatar = null,
                    content = content,
                    published = "",
                    coords = coords,
                    link = link,
                    likeOwnerIds = emptyList(),
                    likedByMe = false,
                    mentionIds = mentionIds,
                    mentionedMe = false,
                    attachment = attachment,
                    ownedByMe = true
                )

                val response = postRepository.save(post)

                if (response != null) {
                    _success.value = true
                } else {
                    _error.value = AppError.ApiError(null, "Ошибка создания поста")
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun updatePost(
        postId: Long,
        content: String,
        link: String? = null,
        coords: Coordinates? = null,
        mentionIds: List<Long> = emptyList(),
        attachment: Attachment? = null
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val existingPost = loadPostForEditing(postId)
                if (existingPost == null) {
                    _error.value = AppError.NotFoundError("Пост не найден")
                    return@launch
                }

                val updatedPost = existingPost.copy(
                    content = content,
                    link = link,
                    coords = coords,
                    mentionIds = mentionIds,
                    attachment = attachment
                )

                val response = postRepository.save(updatedPost)

                if (response != null) {
                    _success.value = true
                } else {
                    _error.value = AppError.ApiError(null, "Ошибка обновления поста")
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }
}