package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import ru.netology.nework.auth.AuthStateManager
import ru.netology.nework.auth.AuthStateManager.authState
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
                findNavController().navigate(
                    R.id.action_postsFragment_to_postDetailFragment,
                    Bundle().apply {
                        putLong("postId", post.id)
                    }
                )
            },
            onMenuClickListener = { post ->
                showPostMenu(post)
            },
            onAttachmentClickListener = { url ->
                showSnackbar("Attachment: $url")
            },
            onLinkClickListener = { url ->
                showSnackbar("Link: $url")
            }
        )
    }
    private fun showPostMenu(post: Post) {
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
            binding.emptyContainer.isVisible = state.empty
            binding.emptyTitle.isVisible = state.empty
            binding.emptySubtitle.isVisible = state.empty
            binding.retryButton.isVisible = state.empty
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing
            if (state.error) {
                showError(state.errorMessage)
            }
            binding.postsList.isEnabled = !state.loading
            binding.fab.isEnabled = !state.loading
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
            if (authState.value?.id != 0L) {
                findNavController().navigate(R.id.action_postsFragment_to_newPostFragment)
            } else {
                showLoginRequiredDialog()
            }
        }
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