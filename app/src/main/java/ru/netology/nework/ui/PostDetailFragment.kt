package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentPostDetailBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.viewmodel.PostDetailViewModel

@AndroidEntryPoint
class PostDetailFragment : Fragment() {

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: Long): PostDetailFragment {
            return PostDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_POST_ID, postId)
                }
            }
        }
    }

    private val viewModel by viewModels<PostDetailViewModel>()
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val mentionsAdapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                // Переход к профилю пользователя
                Snackbar.make(binding.root, "Профиль: ${user.name}", Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupMentionsList()
        setupObservers()
        setupListeners()
        loadPost()
    }

    private fun setupToolbar() {
        // Настройка кнопки "Назад"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupMentionsList() {
        binding.mentionsList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.mentionsList.adapter = mentionsAdapter
    }

    private fun setupObservers() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                updatePostInfo(it)
            }
        }

        viewModel.mentionedUsers.observe(viewLifecycleOwner) { users ->
            mentionsAdapter.submitList(users)
            binding.mentionsList.isVisible = users.isNotEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.contentContainer.isVisible = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { loadPost() }
                    .show()
            }
        }
    }

    private fun setupListeners() {
        // Лайк
        binding.likeButton.setOnClickListener {
            viewModel.post.value?.let { post ->
                viewModel.likePost(post.id)
            }
        }

        // Меню (только для автора)
        binding.menuButton.setOnClickListener {
            showPostMenu()
        }

        // Вложение
        binding.attachmentContainer.setOnClickListener {
            viewModel.post.value?.attachment?.url?.let { url ->
                // TODO: открыть вложение
                Snackbar.make(binding.root, "Открыть: $url", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Ссылка
        binding.linkContainer.setOnClickListener {
            viewModel.post.value?.link?.let { link ->
                // TODO: открыть в браузере
                Snackbar.make(binding.root, "Открыть ссылку: $link", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Карта (если есть координаты)
        binding.mapContainer.setOnClickListener {
            viewModel.post.value?.coords?.let { coords ->
                // TODO: открыть карту
                Snackbar.make(binding.root, "Координаты: ${coords.lat}, ${coords.long}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPost() {
        val postId = arguments?.getLong(ARG_POST_ID) ?: 0L
        if (postId != 0L) {
            lifecycleScope.launch {
                viewModel.loadPost(postId)
            }
        }
    }

    private fun updatePostInfo(post: Post) {
        // Заголовок
        binding.toolbar.title = "Пост от ${post.author}"

        // Аватар автора
        if (!post.authorAvatar.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(post.authorAvatar)
                .circleCrop()
                .placeholder(R.drawable.author_avatar)
                .into(binding.authorAvatar)
        }

        // Информация об авторе
        binding.authorName.text = post.author
        binding.publishedTime.text = post.formattedDate

        // Последнее место работы (по ТЗ)
        binding.authorJob.text = post.authorJob ?: getString(R.string.looking_for_job)
        binding.authorJob.isVisible = true

        // Контент
        binding.content.text = post.content

        // Лайки
        binding.likeCount.text = post.likeOwnerIds.size.toString()
        binding.likeButton.isChecked = post.likedByMe

        // Вложение
        val hasAttachment = post.attachment != null
        binding.attachmentContainer.isVisible = hasAttachment

        if (hasAttachment) {
            post.attachment?.let { attachment ->
                binding.attachmentType.text = when (attachment.type) {
                    AttachmentType.IMAGE -> "Фото"
                    AttachmentType.VIDEO -> "Видео"
                    AttachmentType.AUDIO -> "Аудио"
                }
                binding.attachmentUrl.text = attachment.url
            }
        }

        // Ссылка
        val hasLink = !post.link.isNullOrEmpty()
        binding.linkContainer.isVisible = hasLink

        if (hasLink) {
            binding.linkText.text = post.link
        }

        // Карта (если есть координаты)
        val hasCoords = post.coords != null
        binding.mapContainer.isVisible = hasCoords

        if (hasCoords) {
            binding.coordsText.text = "Координаты: ${post.coords?.lat}, ${post.coords?.long}"
        }
        binding.menuButton.isVisible = post.ownedByMe
    }

    private fun showPostMenu() {
        // TODO: меню редактирования/удаления
        Snackbar.make(binding.root, "Меню поста", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}