package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nework.databinding.CardUserBinding
import ru.netology.nework.dto.User

class UsersAdapter(
    private val onItemClickListener: (User) -> Unit = {}
) : ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = CardUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UserViewHolder(
        private val binding: CardUserBinding,
        private val onItemClickListener: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                // Аватар
                if (!user.avatar.isNullOrEmpty()) {
                    Glide.with(userAvatar)
                        .load(user.avatar)
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(userAvatar)
                } else {
                    userAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                // Информация о пользователе
                userName.text = user.name
                userLogin.text = "@${user.login}"

                // Клик на всю карточку
                root.setOnClickListener {
                    onItemClickListener(user)
                }
            }
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}