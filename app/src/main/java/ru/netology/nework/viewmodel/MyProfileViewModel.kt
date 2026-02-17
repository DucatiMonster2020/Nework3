package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.User
import ru.netology.nework.error.AppError
import ru.netology.nework.repository.JobRepository
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError>()
    val error: LiveData<AppError> = _error

    fun loadMyProfile() {
        viewModelScope.launch {
            try {
                _loading.value = true

                val users = userRepository.getAll()
                _user.value = users.firstOrNull()

            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun saveJob(job: Job) {
        viewModelScope.launch {
            try {
                _loading.value = true
                jobRepository.saveJob(job)
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteJob(jobId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                jobRepository.deleteJob(jobId)
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }
}