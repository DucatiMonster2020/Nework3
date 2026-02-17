package ru.netology.nework.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.PostsAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentPostsBinding
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants.ARG_POST_ID
import ru.netology.nework.viewmodel.PostsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PostsFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel by viewModels<PostsViewModel>()
    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        PostsAdapter(
            onLikeClickListener = { post ->
                viewModel.likeById(post.id)
            },
            onItemClickListener = { post ->
                findNavController().navigate(
                    R.id.action_postsFragment_to_postDetailFragment,
                    Bundle().apply {
                        putLong(ARG_POST_ID, post.id)
                    }
                )
            },
            onMenuClickListener = { post ->
                showPostMenu(post)
            },
            onAttachmentClickListener = { url ->
                openInBrowser(url)
            },
            onLinkClickListener = { url ->
                openInBrowser(url)
            }
        )
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

            binding.emptyContainer.isVisible = state.empty
            binding.emptyTitle.isVisible = state.empty
            binding.emptySubtitle.isVisible = state.empty
            binding.retryButton.isVisible = state.empty
            binding.postsList.isVisible = state.posts.isNotEmpty()

            binding.progressBar.isVisible = state.loading && !state.refreshing
            binding.swipeRefresh.isRefreshing = state.refreshing

            binding.postsList.isEnabled = !state.loading
            binding.fab.isEnabled = !state.loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.fab.setOnClickListener {
            val authState = appAuth.authState.value
            if (authState?.id != 0L) {
                findNavController().navigate(R.id.action_postsFragment_to_newPostFragment)
            } else {
                showLoginRequiredDialog()
            }
        }
    }

    private fun showPostMenu(post: ru.netology.nework.dto.Post) {
        if (!post.ownedByMe) return

        val options = arrayOf(
            getString(R.string.edit_post),
            getString(R.string.delete_post)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.post_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToEditPost(post.id)
                    1 -> confirmDeletePost(post.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToEditPost(postId: Long) {
        findNavController().navigate(
            R.id.action_global_newPostFragment,
            Bundle().apply {
                putLong(ARG_POST_ID, postId)
            }
        )
    }

    private fun confirmDeletePost(postId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post)
            .setMessage(R.string.delete_post_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.removeById(postId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLoginRequiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Требуется вход")
            .setMessage("Для этого действия нужно войти в аккаунт")
            .setPositiveButton("Войти") { _, _ ->
                findNavController().navigate(R.id.action_postsFragment_to_signInFragment)
            }
            .setNegativeButton("Регистрация") { _, _ ->
                findNavController().navigate(R.id.signUpFragment)
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun showError(error: AppError) {
        val message = when (error) {
            is AppError.ApiError -> error.message ?: "Ошибка сервера"
            is AppError.NetworkError -> "Нет соединения с сетью"
            else -> error.message ?: "Неизвестная ошибка"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.loadPosts()
            }
            .show()
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.cannot_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}