package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.CardPostBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType
class PostsAdapter(
    private val onLikeClickListener: (Post) -> Unit,
    private val onItemClickListener: (Post) -> Unit = {},
    private val onMenuClickListener: (Post) -> Unit = {},
    private val onAttachmentClickListener: (String) -> Unit = {},
    private val onLinkClickListener: (String) -> Unit = {}
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(
            binding,
            onLikeClickListener,
            onItemClickListener,
            onMenuClickListener,
            onAttachmentClickListener,
            onLinkClickListener
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        private val binding: CardPostBinding,
        private val onLikeClickListener: (Post) -> Unit,
        private val onItemClickListener: (Post) -> Unit,
        private val onMenuClickListener: (Post) -> Unit,
        private val onAttachmentClickListener: (String) -> Unit,
        private val onLinkClickListener: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.apply {
                // Аватар автора
                if (!post.authorAvatar.isNullOrEmpty()) {
                    Glide.with(authorAvatar)
                        .load(post.authorAvatar)
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(authorAvatar)
                } else {
                    authorAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                // Основная информация
                authorName.text = post.author
                publishedTime.text = post.formattedDate

                // Место работы (если есть)
                if (!post.authorJob.isNullOrEmpty()) {
                    authorJob.text = post.authorJob
                    authorJob.isVisible = true
                } else {
                    authorJob.isVisible = false
                }

                // Контент поста
                content.text = post.content

                // Лайки
                likeCount.text = post.likeOwnerIds.size.toString()
                likeButton.isChecked = post.likedByMe

                // Иконка лайка в зависимости от состояния
                val likeIcon = if (post.likedByMe) {
                    R.drawable.ic_like_filled_24
                } else {
                    R.drawable.ic_like_24
                }
                likeButton.setIconResource(likeIcon)

                // Вложение (аудио, видео, фото)
                val hasAttachment = post.attachment != null
                attachmentContainer.isVisible = hasAttachment

                if (hasAttachment) {
                    post.attachment?.let { attachment ->
                        when (attachment.type) {
                            AttachmentType.IMAGE -> {
                                attachmentIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                                attachmentType.text = "Фото"
                            }
                            AttachmentType.VIDEO -> {
                                attachmentIcon.setImageResource(android.R.drawable.ic_media_play)
                                attachmentType.text = "Видео"
                            }
                            AttachmentType.AUDIO -> {
                                attachmentIcon.setImageResource(android.R.drawable.ic_btn_speak_now)
                                attachmentType.text = "Аудио"
                            }
                        }
                        attachmentUrl.text = attachment.url
                    }
                }

                // Ссылка
                val hasLink = !post.link.isNullOrEmpty()
                linkContainer.isVisible = hasLink

                if (hasLink) {
                    post.link?.let { link ->
                        linkText.text = link
                    }
                }

                // Кнопка меню (видна только автору)
                menuButton.isVisible = post.ownedByMe

                // Упомянутые пользователи (если есть)
                val hasMentions = !post.mentionIds.isNullOrEmpty()
                mentionsContainer.isVisible = hasMentions

                if (hasMentions && post.mentionIds != null) {
                    mentionsCount.text = "Упомянуто: ${post.mentionIds.size} чел."
                    mentionsMeIndicator.isVisible = post.mentionedMe
                }

                // ========== ОБРАБОТЧИКИ КЛИКОВ ==========

                // Лайк
                likeButton.setOnClickListener {
                    onLikeClickListener(post)
                }

                // Меню
                menuButton.setOnClickListener {
                    onMenuClickListener(post)
                }

                // Вложение
                if (hasAttachment) {
                    attachmentContainer.setOnClickListener {
                        post.attachment?.url?.let { url ->
                            onAttachmentClickListener(url)
                        }
                    }
                }

                // Ссылка
                if (hasLink) {
                    linkContainer.setOnClickListener {
                        post.link?.let { link ->
                            onLinkClickListener(link)
                        }
                    }
                }

                // Вся карточка
                root.setOnClickListener {
                    onItemClickListener(post)
                }

                // Переменные для передачи данных в обработчики
                root.tag = post
                attachmentContainer.tag = post.attachment?.url
                linkContainer.tag = post.link
            }
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}