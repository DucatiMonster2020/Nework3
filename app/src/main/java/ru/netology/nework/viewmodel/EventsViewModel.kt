package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.error.AppError
import ru.netology.nework.model.FeedModel
import ru.netology.nework.repository.EventRepository
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel> = _dataState

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    init {
        loadEvents()
    }

    fun loadEvents() {
        viewModelScope.launch {
            try {
                _dataState.value = _dataState.value?.copy(loading = true)
                val events = repository.getAll()
                _dataState.value = FeedModel(
                    events = events,
                    empty = events.isEmpty(),
                    loading = false
                )
            } catch (e: Exception) {
                _dataState.value = _dataState.value?.copy(loading = false)
                _error.value = AppError.fromThrowable(e)
            }
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            try {
                _dataState.value = _dataState.value?.copy(refreshing = true)
                val events = repository.getAll()
                _dataState.value = FeedModel(
                    events = events,
                    empty = events.isEmpty(),
                    refreshing = false
                )
            } catch (e: Exception) {
                _dataState.value = _dataState.value?.copy(refreshing = false)
                _error.value = AppError.fromThrowable(e)
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
                _error.value = AppError.fromThrowable(e)
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
                _error.value = AppError.fromThrowable(e)
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
                _error.value = AppError.fromThrowable(e)
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
                _error.value = AppError.fromThrowable(e)
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
                _error.value = AppError.fromThrowable(e)
                loadEvents()
            }
        }
    }
}