package ru.netology.nework.model

import ru.netology.nework.dto.Event
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val events: List<Event> = emptyList(),
    val users: List<User> = emptyList(),
    val empty: Boolean = false,
)

data class FeedModelState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: Boolean = false,
    val errorMessage: String? = null,
    val empty: Boolean = false
) {
    companion object {
        val IDLE = FeedModelState()
        val LOADING = FeedModelState(loading = true)
        val REFRESHING = FeedModelState(refreshing = true)
        fun error(message: String? = null) = FeedModelState(error = true, errorMessage = message)
        val EMPTY = FeedModelState(empty = true)
    }
}