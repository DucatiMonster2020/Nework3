package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData(false)
    val success: LiveData<Boolean> = _success

    suspend fun loadUsersByIds(userIds: List<Long>): List<User> {
        return try {
            val allUsers = userRepository.getAll()
            allUsers.filter { user -> userIds.contains(user.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadMedia(uri: Uri, type: AttachmentType): String {
        return try {
            _loading.postValue(true)
            val response = mediaRepository.upload(uri, type)
            response.url
        } catch (e: Exception) {
            throw e
        } finally {
            _loading.postValue(false)
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
                _error.value = null
                _success.value = false
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
                    _error.value = "Ошибка создания поста"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            } finally {
                _loading.value = false
            }
        }
    }
}