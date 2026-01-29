package ru.netology.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Event
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.error.AppError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class NewEventViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData(false)
    val success: LiveData<Boolean> = _success

    fun saveEvent(
        content: String,
        datetime: Date,
        isOnline: Boolean,
        link: String? = null,
        attachmentUri: Uri? = null,
        attachmentType: String? = null
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _success.value = false

                // Форматируем дату для API
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val datetimeString = dateFormat.format(datetime)

                // Создаем событие
                val event = Event(
                    id = 0,
                    author = "", // Заполнится на сервере
                    authorId = 0, // Заполнится на сервере
                    authorJob = null,
                    authorAvatar = null,
                    content = content,
                    datetime = datetimeString,
                    published = "", // Заполнится на сервере
                    coords = null,
                    type = if (isOnline) EventType.ONLINE else EventType.OFFLINE,
                    likeOwnerIds = emptyList(),
                    likedByMe = false,
                    speakerIds = emptyList(), // TODO: добавить спикеров
                    participantsIds = emptyList(),
                    participatedByMe = false,
                    attachment = null, // TODO: добавить attachment
                    link = link,
                    ownedByMe = true
                )

                val response = apiService.saveEvent(event)

                if (response.isSuccessful) {
                    _success.value = true
                } else {
                    _error.value = "Ошибка создания события: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            } finally {
                _loading.value = false
            }
        }
    }
}