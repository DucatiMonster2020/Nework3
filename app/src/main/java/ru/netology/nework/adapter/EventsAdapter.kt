package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.CardEventBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType

class EventsAdapter(
    private val onLikeClickListener: (Event) -> Unit,
    private val onItemClickListener: (Event) -> Unit = {},
    private val onMenuClickListener: (Event) -> Unit = {},
    private val onParticipateClickListener: (Event) -> Unit = {},
    private val onAttachmentClickListener: (String) -> Unit = {},
    private val onLinkClickListener: (String) -> Unit = {}
) : ListAdapter<Event, EventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = CardEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(
            binding,
            onLikeClickListener,
            onItemClickListener,
            onMenuClickListener,
            onParticipateClickListener,
            onAttachmentClickListener,
            onLinkClickListener
        )
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventViewHolder(
        private val binding: CardEventBinding,
        private val onLikeClickListener: (Event) -> Unit,
        private val onItemClickListener: (Event) -> Unit,
        private val onMenuClickListener: (Event) -> Unit,
        private val onParticipateClickListener: (Event) -> Unit,
        private val onAttachmentClickListener: (String) -> Unit,
        private val onLinkClickListener: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                // Аватар автора
                if (!event.authorAvatar.isNullOrEmpty()) {
                    Glide.with(authorAvatar)
                        .load(event.authorAvatar)
                        .circleCrop()
                        .placeholder(R.drawable.author_avatar)
                        .into(authorAvatar)
                } else {
                    authorAvatar.setImageResource(R.drawable.author_avatar)
                }

                // Основная информация
                authorName.text = event.author
                publishedTime.text = event.formattedPublished

                // Место работы (если есть)
                if (!event.authorJob.isNullOrEmpty()) {
                    authorJob.text = event.authorJob
                    authorJob.isVisible = true
                } else {
                    authorJob.isVisible = false
                }

                // Дата и время проведения
                eventDateTime.text = event.formattedDateTime

                // Тип события (ONLINE/OFFLINE)
                eventType.text = when (event.type) {
                    EventType.ONLINE -> "ONLINE"
                    EventType.OFFLINE -> "OFFLINE"
                }

                // Цвет в зависимости от типа
                eventType.setBackgroundResource(
                    when (event.type) {
                        EventType.ONLINE -> R.color.event_online
                        EventType.OFFLINE -> R.color.event_offline
                    }
                )

                // Контент события
                content.text = event.content

                // Лайки
                likeCount.text = event.likeOwnerIds.size.toString()
                likeButton.isChecked = event.likedByMe

                // Участие
                participantsCount.text = event.participantsIds.size.toString()
                participateButton.isChecked = event.participatedByMe
                val participateIcon = if (event.participatedByMe) {
                    R.drawable.ic_check
                } else {
                    R.drawable.ic_check_box_outline
                }
                participateButton.setIconResource(participateIcon)

                // Спикеры
                speakersCount.text = event.speakerIds.size.toString()

                // Вложение
                val hasAttachment = event.attachment != null
                attachmentContainer.isVisible = hasAttachment

                if (hasAttachment) {
                    event.attachment?.let { attachment ->
                        when (attachment.type) {
                            AttachmentType.IMAGE -> {
                                attachmentIcon.setImageResource(R.drawable.ic_image)
                                attachmentType.text = "Фото"
                            }
                            AttachmentType.VIDEO -> {
                                attachmentIcon.setImageResource(R.drawable.ic_video)
                                attachmentType.text = "Видео"
                            }
                            AttachmentType.AUDIO -> {
                                attachmentIcon.setImageResource(R.drawable.ic_audio)
                                attachmentType.text = "Аудио"
                            }
                        }
                        attachmentUrl.text = attachment.url
                    }
                }

                // Ссылка
                val hasLink = !event.link.isNullOrEmpty()
                linkContainer.isVisible = hasLink

                if (hasLink) {
                    event.link?.let { link ->
                        linkText.text = link
                    }
                }

                // Кнопка меню (видна только автору)
                menuButton.isVisible = event.ownedByMe

                // ========== ОБРАБОТЧИКИ КЛИКОВ ==========

                // Лайк
                likeButton.setOnClickListener {
                    onLikeClickListener(event)
                }

                // Участие
                participateButton.setOnClickListener {
                    onParticipateClickListener(event)
                }

                // Меню
                menuButton.setOnClickListener {
                    onMenuClickListener(event)
                }

                // Вложение
                if (hasAttachment) {
                    attachmentContainer.setOnClickListener {
                        event.attachment?.url?.let { url ->
                            onAttachmentClickListener(url)
                        }
                    }
                }

                // Ссылка
                if (hasLink) {
                    linkContainer.setOnClickListener {
                        event.link?.let { link ->
                            onLinkClickListener(link)
                        }
                    }
                }

                // Вся карточка
                root.setOnClickListener {
                    onItemClickListener(event)
                }
            }
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
        return oldItem == newItem
    }
}