package ru.netology.nework.model

import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.error.AppError

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val events: List<Event> = emptyList(),
    val users: List<User> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: AppError? = null,
    val empty: Boolean = false
) {
    val isLoading: Boolean get() = loading || refreshing

    companion object {
        val IDLE = FeedModel()
        fun loading() = FeedModel(loading = true)
        fun refreshing() = FeedModel(refreshing = true)
        fun error(error: AppError) = FeedModel(error = error)
    }
}