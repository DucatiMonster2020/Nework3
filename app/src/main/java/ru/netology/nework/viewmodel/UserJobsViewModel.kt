package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Job
import ru.netology.nework.error.AppError
import javax.inject.Inject

@HiltViewModel
class UserJobsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    private val _jobs = MutableLiveData<List<Job>>(emptyList())
    val jobs: LiveData<List<Job>> = _jobs

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadJobs(userId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                // Пока используем getMyJobs() для демонстрации
                // TODO: Нужно добавить endpoint для получения работ пользователя
                val response = apiService.getMyJobs()

                if (response.isSuccessful) {
                    val allJobs = response.body() ?: emptyList()
                    // Показываем все работы (в реальном приложении фильтровать по userId)
                    _jobs.value = allJobs
                } else {
                    _error.value = AppError.fromThrowable(
                        retrofit2.HttpException(response)
                    ).message
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshJobs(userId: Long) {
        loadJobs(userId)
    }

    fun deleteJob(jobId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteJob(jobId)
                if (response.isSuccessful) {
                    // Удаляем работу из списка
                    _jobs.value = _jobs.value?.filter { it.id != jobId }
                } else {
                    _error.value = "Не удалось удалить работу"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            }
        }
    }
}