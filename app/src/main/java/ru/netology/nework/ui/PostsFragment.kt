package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.adapter.PostsAdapter
import ru.netology.nework.auth.AuthStateManager
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.viewmodel.PostsViewModel

@AndroidEntryPoint
class PostsFragment : Fragment() {

    private val viewModel by viewModels<PostsViewModel>()
    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        PostsAdapter(
            onLikeClickListener = { post ->
                viewModel.likeById(post.id)
            },
            onItemClickListener = { post ->
                // TODO: переход к деталям
                showSnackbar("Clicked post: ${post.id}")
            },
            onMenuClickListener = { post ->
                // TODO: показать меню удаления/редактирования
                showPostMenu(post)
            },
            onAttachmentClickListener = { url ->
                showSnackbar("Attachment: $url")
                // TODO: открыть вложение
            },
            onLinkClickListener = { url ->
                showSnackbar("Link: $url")
                // TODO: открыть в браузере
            }
        )
    }
    private fun showPostMenu(post: Post) {
        // TODO: показать диалог с опциями удалить/редактировать
        showSnackbar("Menu for post ${post.id}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.postsList.layoutManager = LinearLayoutManager(requireContext())
        binding.postsList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)

            // Показываем/скрываем сообщение о пустом списке
            binding.emptyContainer.isVisible = state.empty
            binding.emptyTitle.isVisible = state.empty
            binding.emptySubtitle.isVisible = state.empty

            // Показываем/скрываем кнопку Retry при ошибке
            binding.retryButton.isVisible = state.empty
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            // Состояние загрузки
            binding.progressBar.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing
            if (state.error) {
                showError(state.errorMessage)
            }

            // Блокируем/разблокируем UI в зависимости от состояния
            binding.postsList.isEnabled = !state.loading
            binding.fab.isEnabled = !state.loading
        }
    }

    private fun setupListeners() {
        // Обновление по свайпу
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        // Кнопка Retry
        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        // FAB для создания поста
        binding.fab.setOnClickListener {
            val isAuthorized = AuthStateManager.authState.value is AuthStateManager.AuthState.Authorized
            if (isAuthorized) {
                showSnackbar("Create new post")
            } else {
                showLoginRequiredDialog()
            }
        }
    }
    private fun showLoginRequiredDialog() {
        showSnackbar("Please sign in to create new post")
    }

    private fun showError(message: String?) {
        val errorMsg = message ?: "An error occurred"
        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                viewModel.loadPosts()
            }
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}