package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        private val onAttachmentClickListener: (String) -> Unit,
        private val onLinkClickListener: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                event.authorAvatar?.let { avatar ->
                    if (avatar.isNotBlank()) {
                        Glide.with(authorAvatar)
                            .load(avatar)
                            .circleCrop()
                            .placeholder(R.drawable.author_avatar)
                            .into(authorAvatar)
                    } else {
                        authorAvatar.setImageResource(R.drawable.author_avatar)
                    }
                } ?: run {
                    authorAvatar.setImageResource(R.drawable.author_avatar)
                }
                authorName.text = event.author
                publishedTime.text = event.formattedPublished
                eventType.text = when (event.type) {
                    EventType.ONLINE -> "ONLINE"
                    EventType.OFFLINE -> "OFFLINE"
                }
                val typeColor = when (event.type) {
                    EventType.ONLINE -> R.color.event_online
                    EventType.OFFLINE -> R.color.event_offline
                }
                eventType.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, typeColor)
                )
                eventDateTime.text = event.formattedDateTime
                content.text = event.content
                likeCount.text = event.likeOwnerIds.size.toString()
                val likeIcon = if (event.likedByMe) {
                    R.drawable.ic_like_filled_24
                } else {
                    R.drawable.ic_like_24
                }
                likeButton.setImageResource(likeIcon)
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
                val hasLink = !event.link.isNullOrBlank()
                linkContainer.isVisible = hasLink
                if (hasLink) {
                    linkText.text = event.link
                }
                menuButton.isVisible = event.ownedByMe
                likeButton.setOnClickListener { onLikeClickListener(event) }
                menuButton.setOnClickListener { onMenuClickListener(event) }

                if (hasAttachment) {
                    attachmentContainer.setOnClickListener {
                        event.attachment?.url?.let { url -> onAttachmentClickListener(url) }
                    }
                }

                if (hasLink) {
                    linkContainer.setOnClickListener {
                        event.link?.let { link -> onLinkClickListener(link) }
                    }
                }

                root.setOnClickListener { onItemClickListener(event) }
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
}