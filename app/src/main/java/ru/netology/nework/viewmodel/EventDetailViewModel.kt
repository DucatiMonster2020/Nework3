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
import ru.netology.nework.repository.EventRepository
import ru.netology.nework.utils.Constants.ERROR_LOAD_EVENT
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val apiService: ApiService,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _speakers = MutableLiveData<List<User>>(emptyList())
    val speakers: LiveData<List<User>> = _speakers

    private val _participants = MutableLiveData<List<User>>(emptyList())
    val participants: LiveData<List<User>> = _participants

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val response = apiService.getEventById(eventId)
                if (response.isSuccessful) {
                    val event = response.body()
                    _event.value = event
                    loadSpeakersAndParticipants(event)
                } else {
                    _error.value = AppError.ApiError(response.code(), ERROR_LOAD_EVENT)
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun loadSpeakersAndParticipants(event: Event?) {
        try {
            val response = apiService.getAllUsers()
            if (response.isSuccessful) {
                val allUsers = response.body() ?: emptyList()

                val speakerUsers = allUsers.filter { user ->
                    event?.speakerIds?.contains(user.id) == true
                }
                _speakers.value = speakerUsers

                val participantUsers = allUsers.filter { user ->
                    event?.participantsIds?.contains(user.id) == true
                }
                _participants.value = participantUsers
            }
        } catch (e: Exception) {
        }
    }

    fun likeEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val response = eventRepository.likeById(eventId)
                if (response != null) {
                    _event.value = response
                    loadEvent(eventId)
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }

    fun participateEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                val response = if (_event.value?.participatedByMe == true) {
                    eventRepository.cancelParticipation(eventId)
                } else {
                    eventRepository.participate(eventId)
                }
                if (response != null) {
                    _event.value = response
                    loadEvent(eventId)
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                eventRepository.removeById(eventId)
                _event.value = null
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }
}