package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nework.api.ApiService
import ru.netology.nework.dto.User
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.SingleLiveEvent
import javax.inject.Inject

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = SingleLiveEvent<AppError?>()
    val error: LiveData<AppError?> = _error

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val response = apiService.getUserById(userId)
                if (response.isSuccessful) {
                    _user.value = response.body()
                } else {
                    _error.value = AppError.fromThrowable(
                        HttpException(response)
                    )
                }
            } catch (e: Exception) {
                _error.value = AppError.fromThrowable(e)
            } finally {
                _loading.value = false
            }
        }
    }
}