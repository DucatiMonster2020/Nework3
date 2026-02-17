package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.R
import ru.netology.nework.databinding.CardUserBinding
import ru.netology.nework.dto.User

class UsersAdapter(
    private val onItemClickListener: (User) -> Unit,
    private val isSelectionMode: Boolean = false,
    private var selectedIds: Set<Long> = emptySet()
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UsersDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = CardUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onItemClickListener, isSelectionMode)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        val isSelected = selectedIds.contains(user.id)
        holder.bind(user, isSelected)
    }

    fun updateSelectedIds(newSelectedIds: Set<Long>) {
        selectedIds = newSelectedIds
        notifyItemRangeChanged(0, itemCount)
    }

    class UserViewHolder(
        private val binding: CardUserBinding,
        private val onItemClickListener: (User) -> Unit,
        private val isSelectionMode: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentUser: User? = null

        init {
            binding.root.setOnClickListener {
                currentUser?.let { user ->
                    if (isSelectionMode) {
                        binding.selectionIndicator.performClick()
                    } else {
                        onItemClickListener(user)
                    }
                }
            }
        }

        fun bind(user: User, isSelected: Boolean) {
            currentUser = user

            binding.apply {
                userName.text = user.name
                userLogin.text = "@${user.login}"

                if (!user.avatar.isNullOrEmpty()) {
                    Glide.with(binding.root)
                        .load(user.avatar).circleCrop()
                        .placeholder(R.drawable.author_avatar)
                        .error(R.drawable.author_avatar)
                        .into(userAvatar)
                } else {
                    userAvatar.setImageResource(R.drawable.author_avatar)
                }
                selectionIndicator.isVisible = isSelectionMode

                if (isSelectionMode) {
                    selectionIndicator.isChecked = isSelected
                }
            }
        }
    }

    class UsersDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}