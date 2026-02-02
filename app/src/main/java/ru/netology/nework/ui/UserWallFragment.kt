package ru.netology.nework.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.PostsAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUserWallBinding
import ru.netology.nework.dto.Post
import ru.netology.nework.viewmodel.UserWallViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserWallFragment : Fragment() {

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_IS_CURRENT_USER = "is_current_user"

        fun newInstance(userId: Long, isCurrentUser: Boolean = false): UserWallFragment {
            return UserWallFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                    putBoolean(ARG_IS_CURRENT_USER, isCurrentUser)
                }
            }
        }
    }

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel by viewModels<UserWallViewModel>()
    private var _binding: FragmentUserWallBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        PostsAdapter(
            onLikeClickListener = { post ->
                viewModel.likeById(post.id)
            },
            onItemClickListener = { post ->
                // TODO: переход к деталям поста
                showSnackbar("Пост ${post.id}")
            },
            onMenuClickListener = { post ->
                showPostMenu(post)
            },
            onAttachmentClickListener = { url ->
                openLinkInBrowser(url)
            },
            onLinkClickListener = { url ->
                openLinkInBrowser(url)
            }
        )
    }

    private var userId: Long = 0
    private var isCurrentUser: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getLong(ARG_USER_ID)
            isCurrentUser = it.getBoolean(ARG_IS_CURRENT_USER, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserWallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        loadUserWall()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.title = getString(R.string.user_wall)
        }

        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().popBackStack()) {
                activity?.onBackPressed()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.postsList.layoutManager = LinearLayoutManager(requireContext())
        binding.postsList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.emptyState.isVisible = posts.isEmpty()
            binding.postsList.isVisible = posts.isNotEmpty()
        }
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                updateUserInfo(it)
            }
        }

        viewModel.lastJob.observe(viewLifecycleOwner) { job ->
            binding.userJob.text = job ?: getString(R.string.looking_for_job)
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.swipeRefresh.isRefreshing = loading

            if (!loading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
            }
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshUserWall()
        }
        binding.fab.apply {
            isVisible = isCurrentUser
            setOnClickListener {
                if (appAuth.authState.value?.id != 0L) {
                    navigateToCreatePost()
                } else {
                    showLoginRequiredDialog()
                }
            }
        }

        binding.retryButton.setOnClickListener {
            loadUserWall()
        }
    }

    private fun loadUserWall() {
        lifecycleScope.launch {
            viewModel.loadUserWall(userId)
        }
    }

    private fun refreshUserWall() {
        lifecycleScope.launch {
            viewModel.refreshUserWall(userId)
        }
    }

    private fun updateUserInfo(user: ru.netology.nework.dto.User) {
        if (isAdded) {
            (activity as? AppCompatActivity)?.supportActionBar?.title = user.name
            binding.toolbar.subtitle = "@${user.login}"
        }
        if (!user.avatar.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(user.avatar)
                .circleCrop()
                .placeholder(R.drawable.author_avatar)
                .error(R.drawable.author_avatar)
                .into(binding.userAvatar)
        } else {
            binding.userAvatar.setImageResource(R.drawable.author_avatar)
        }
        binding.userName.text = user.name
        binding.userLogin.text = "@${user.login}"
    }

    private fun showPostMenu(post: Post) {
        val canEdit = post.ownedByMe || isCurrentUser

        if (canEdit) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.post_options))
                .setItems(arrayOf(
                    getString(R.string.edit_post),
                    getString(R.string.delete_post)
                )) { _, which ->
                    when (which) {
                        0 -> navigateToEditPost(post.id)
                        1 -> deletePost(post.id)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
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

    private fun navigateToCreatePost() {
        showSnackbar("Создать новый пост")
    }

    private fun navigateToEditPost(postId: Long) {
        showSnackbar("Редактировать пост $postId")
    }

    private fun deletePost(postId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post)
            .setMessage(R.string.delete_post_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deletePost(postId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openLinkInBrowser(url: String) {
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                Uri.parse(url)
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.cannot_open_link,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showError(message: String) {
        if (isAdded) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry) { loadUserWall() }
                .show()
        }
    }

    private fun showSnackbar(message: String) {
        if (isAdded) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserWall()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}