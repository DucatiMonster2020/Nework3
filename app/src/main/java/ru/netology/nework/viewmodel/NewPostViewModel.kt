package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Post
import ru.netology.nework.error.AppError
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData(false)
    val success: LiveData<Boolean> = _success

    fun savePost(
        content: String,
        link: String? = null,
        attachmentUri: Uri? = null,
        attachmentType: String? = null
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _success.value = false

                // Создаем пост (без вложения пока)
                val post = Post(
                    id = 0,
                    author = "", // Заполнится на сервере
                    authorId = 0, // Заполнится на сервере
                    content = content,
                    published = "", // Заполнится на сервере
                    link = link
                    // TODO: добавить attachment когда будет готово
                )

                val response = apiService.savePost(post)

                if (response.isSuccessful) {
                    _success.value = true
                } else {
                    _error.value = "Ошибка создания поста: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            } finally {
                _loading.value = false
            }
        }
    }
}