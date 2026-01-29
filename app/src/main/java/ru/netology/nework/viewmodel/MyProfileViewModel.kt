package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.User
import ru.netology.nework.error.AppError
import javax.inject.Inject

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val response = apiService.getUserById(userId)
                if (response.isSuccessful) {
                    _user.value = response.body()
                } else {
                    _error.value = "Не удалось загрузить профиль"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteJob(jobId: Long) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteJob(jobId)
                if (response.isSuccessful) {
                    // TODO: Обновить список работ в табе
                } else {
                    _error.value = "Не удалось удалить работу"
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e).message
            }
        }
    }
}