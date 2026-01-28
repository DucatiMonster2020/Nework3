package ru.netology.nework.dto

import ru.netology.nework.enumeration.EventType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Event(
    val id: Long,
    val author: String,
    val authorId: Long,
    val authorJob: String? = null,
    val authorAvatar: String? = null,
    val content: String,
    val datetime: String,
    val published: String,
    val coords: Coordinates? = null,
    val type: EventType,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean = false,
    val speakerIds: List<Long> = emptyList(),
    val participantsIds: List<Long> = emptyList(),
    val participatedByMe: Boolean = false,
    val attachment: Attachment? = null,
    val link: String? = null,
    val ownedByMe: Boolean = false,
) {
    val formattedDateTime: String
        get() = try {
            val instant = Instant.parse(datetime)
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            datetime
        }
    val formattedPublished: String
        get() = try {
            val instant = Instant.parse(published)
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            published
        }
}