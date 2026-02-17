package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.Job
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class UserJobsViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _jobs = MutableLiveData<List<Job>>(emptyList())
    val jobs: LiveData<List<Job>> = _jobs

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    private var currentUserId: Long = 0

    fun loadJobs(userId: Long) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                _loading.value = true

                val response = if (userId == 0L) {
                    apiService.getMyJobs()
                } else {
                    apiService.getUserJobs(userId)
                }

                if (response.isSuccessful) {
                    _jobs.value = response.body() ?: emptyList()
                } else {
                    _error.value = AppError.fromThrowable(HttpException(response))
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshJobs() {
        if (currentUserId != 0L) {
            loadJobs(currentUserId)
        }
    }

    fun deleteJob(jobId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteJob(jobId)
                if (response.isSuccessful) {
                    val currentJobs = _jobs.value ?: emptyList()
                    _jobs.value = currentJobs.filter { it.id != jobId }
                } else {
                    _error.value = AppError.ApiError(
                        code = response.code(),
                        message = "Не удалось удалить работу"
                    )
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            }
        }
    }
}