package ru.netology.nework.utils

object Constants {
    const val ARG_POST_ID = "postId"
    const val ARG_EVENT_ID = "eventId"
    const val ARG_USER_ID = "userId"
    const val ARG_IS_CURRENT_USER = "isCurrentUser"

    const val LOCATION_REQUEST_KEY = "location_request_key"
    const val LOCATION_LAT = "lat"
    const val LOCATION_LNG = "lng"

    const val MAX_FILE_SIZE = 15 * 1024 * 1024
    const val MAX_IMAGE_SIZE = 2048 * 2048

    const val ERROR_INVALID_LOGIN_PASSWORD = "Неправильный логин или пароль"
    const val ERROR_USER_ALREADY_EXISTS = "Пользователь с таким логином уже зарегистрирован"
    const val ERROR_LOAD_POST = "Не удалось загрузить пост"
    const val ERROR_LOAD_EVENT = "Не удалось загрузить событие"
    const val ERROR_LOAD_PROFILE = "Не удалось загрузить профиль"
    const val ERROR_LOAD_WALL = "Не удалось загрузить стену пользователя"
    const val ERROR_LIKE = "Не удалось поставить лайк"
    const val ERROR_DELETE = "Не удалось удалить"
}