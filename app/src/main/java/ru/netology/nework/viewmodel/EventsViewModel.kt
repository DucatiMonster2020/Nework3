package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.model.FeedModel
import ru.netology.nework.model.FeedModelState
import ru.netology.nework.repository.EventRepository
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel> = _dataState

    private val _state = MutableLiveData<FeedModelState>(FeedModelState.IDLE)
    val state: LiveData<FeedModelState> = _state

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState.LOADING
                val events = repository.getAll()
                _dataState.value = FeedModel(
                    events = events,
                    empty = events.isEmpty()
                )
                _state.value = FeedModelState.IDLE
            } catch (e: Exception) {
                _state.value = FeedModelState.error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            try {
                _state.value = FeedModelState.REFRESHING
                val events = repository.getAll()
                _dataState.value = FeedModel(
                    events = events,
                    empty = events.isEmpty()
                )
                _state.value = FeedModelState.IDLE
            } catch (e: Exception) {
                _state.value = FeedModelState.error(e.message ?: "Unknown error")
            }
        }
    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            try {
                val event = repository.likeById(id)
                val currentEvents = _dataState.value?.events ?: emptyList()
                val newEvents = currentEvents.map { if (it.id == id) event else it }
                _dataState.value = _dataState.value?.copy(events = newEvents)
            } catch (e: Exception) {
                _state.value = FeedModelState.error("Failed to like event")
                loadEvents()
            }
        }
    }

    fun dislikeById(id: Long) {
        viewModelScope.launch {
            try {
                val event = repository.dislikeById(id)
                val currentEvents = _dataState.value?.events ?: emptyList()
                val newEvents = currentEvents.map { if (it.id == id) event else it }
                _dataState.value = _dataState.value?.copy(events = newEvents)
            } catch (e: Exception) {
                _state.value = FeedModelState.error("Failed to dislike event")
                loadEvents()
            }
        }
    }

    fun participateById(id: Long) {
        viewModelScope.launch {
            try {
                val event = repository.participate(id)
                val currentEvents = _dataState.value?.events ?: emptyList()
                val newEvents = currentEvents.map { if (it.id == id) event else it }
                _dataState.value = _dataState.value?.copy(events = newEvents)
            } catch (e: Exception) {
                _state.value = FeedModelState.error("Failed to participate")
                loadEvents()
            }
        }
    }

    fun cancelParticipationById(id: Long) {
        viewModelScope.launch {
            try {
                val event = repository.cancelParticipation(id)
                val currentEvents = _dataState.value?.events ?: emptyList()
                val newEvents = currentEvents.map { if (it.id == id) event else it }
                _dataState.value = _dataState.value?.copy(events = newEvents)
            } catch (e: Exception) {
                _state.value = FeedModelState.error("Failed to cancel participation")
                loadEvents()
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
                val currentEvents = _dataState.value?.events ?: emptyList()
                val newEvents = currentEvents.filter { it.id != id }
                _dataState.value = _dataState.value?.copy(events = newEvents)
            } catch (e: Exception) {
                _state.value = FeedModelState.error("Failed to delete event")
                loadEvents()
            }
        }
    }
}