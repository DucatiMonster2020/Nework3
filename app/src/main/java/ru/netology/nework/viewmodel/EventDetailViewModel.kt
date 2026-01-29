package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.error.AppError
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _speakers = MutableLiveData<List<User>>(emptyList())
    val speakers: LiveData<List<User>> = _speakers

    private val _participants = MutableLiveData<List<User>>(emptyList())
    val participants: LiveData<List<User>> = _participants

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val response = apiService.getEventById(eventId)
                if (response.isSuccessful) {
                    val event = response.body()
                    _event.value = event

                    // Загружаем спикеров и участников
                    event?.speakerIds?.let { loadSpeakers(it) }
                    event?.participantsIds?.let { loadParticipants(it) }
                } else {
                    _error.value = "Не удалось загрузить событие"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadSpeakers(userIds: List<Long>) {
        try {
            val response = apiService.getAllUsers()
            if (response.isSuccessful) {
                val allUsers = response.body() ?: emptyList()
                val speakerUsers = allUsers.filter { user -> userIds.contains(user.id) }
                _speakers.value = speakerUsers
            }
        } catch (e: Exception) {
            // Игнорируем ошибку
        }
    }

    private suspend fun loadParticipants(userIds: List<Long>) {
        try {
            val response = apiService.getAllUsers()
            if (response.isSuccessful) {
                val allUsers = response.body() ?: emptyList()
                val participantUsers = allUsers.filter { user -> userIds.contains(user.id) }
                _participants.value = participantUsers
            }
        } catch (e: Exception) {
            // Игнорируем ошибку
        }
    }
    fun likeEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.likeEvent(eventId)
                if (response.isSuccessful) {
                    _event.value = response.body()
                }
            } catch (e: Exception) {
                _error.value = "Не удалось поставить лайк"
            }
        }
    }

    fun participateEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                // TODO: Нужен endpoint для участия в событии
                // Пока просто обновляем UI
                _event.value?.let { currentEvent ->
                    val newEvent = currentEvent.copy(
                        participatedByMe = !currentEvent.participatedByMe,
                        participantsIds = if (currentEvent.participatedByMe) {
                            currentEvent.participantsIds - 999 // ID текущего пользователя
                        } else {
                            currentEvent.participantsIds + 999
                        }
                    )
                    _event.value = newEvent
                }
            } catch (e: Exception) {
                _error.value = "Не удалось изменить участие"
            }
        }
    }
}