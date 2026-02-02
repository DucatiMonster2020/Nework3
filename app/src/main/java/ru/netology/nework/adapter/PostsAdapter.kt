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
                if (!post.authorAvatar.isNullOrEmpty()) {
                    Glide.with(authorAvatar)
                        .load(post.authorAvatar)
                        .circleCrop()
                        .placeholder(R.drawable.author_avatar)
                        .into(authorAvatar)
                } else {
                    authorAvatar.setImageResource(R.drawable.author_avatar)
                }
                authorName.text = post.author
                publishedTime.text = post.formattedDate
                content.text = post.content
                likeCount.text = post.likeOwnerIds.size.toString()
                val likeIcon = if (post.likedByMe) {
                    R.drawable.ic_like_filled_24
                } else {
                    R.drawable.ic_like_24
                }
                likeButton.setImageResource(likeIcon)
                val hasAttachment = post.attachment != null
                attachmentContainer.isVisible = hasAttachment

                if (hasAttachment) {
                    post.attachment?.let { attachment ->
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
                val hasLink = !post.link.isNullOrEmpty()
                linkContainer.isVisible = hasLink

                if (hasLink) {
                    linkText.text = post.link
                }
                menuButton.isVisible = post.ownedByMe
                likeButton.setOnClickListener {
                    onLikeClickListener(post)
                }
                menuButton.setOnClickListener {
                    onMenuClickListener(post)
                }
                if (hasAttachment) {
                    attachmentContainer.setOnClickListener {
                        post.attachment?.url?.let { url ->
                            onAttachmentClickListener(url)
                        }
                    }
                }
                if (hasLink) {
                    linkContainer.setOnClickListener {
                        post.link?.let { link ->
                            onLinkClickListener(link)
                        }
                    }
                }
                root.setOnClickListener {
                    onItemClickListener(post)
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
}