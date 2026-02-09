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
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.SingleLiveEvent
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

    private val _error = SingleLiveEvent<AppError?>()
    val error: LiveData<AppError?> = _error

    private val _success = SingleLiveEvent<Boolean>()
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

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val datetimeString = dateFormat.format(datetime)

                val event = Event(
                    id = 0,
                    author = "",
                    authorId = 0,
                    authorJob = null,
                    authorAvatar = null,
                    content = content,
                    datetime = datetimeString,
                    published = "",
                    coords = null,
                    type = if (isOnline) EventType.ONLINE else EventType.OFFLINE,
                    likeOwnerIds = emptyList(),
                    likedByMe = false,
                    speakerIds = emptyList(),
                    participantsIds = emptyList(),
                    participatedByMe = false,
                    attachment = null,
                    link = link,
                    ownedByMe = true
                )

                val response = apiService.saveEvent(event)

                if (response.isSuccessful) {
                    _success.value = true
                } else {
                    _error.value = ApiError("Ошибка создания события: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }
}