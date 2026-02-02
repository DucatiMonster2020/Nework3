package ru.netology.nework.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentPostDetailBinding
import ru.netology.nework.dto.Coordinates
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
                findNavController().navigate(R.id.action_global_userDetailFragment,
                    Bundle().apply {
                        putLong("userId", user.id)
                    }
                )
            }
        )
    }

    private var isMapInitialized = false

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
        binding.likeButton.setOnClickListener {
            viewModel.post.value?.let { post ->
                viewModel.likePost(post.id)
            }
        }

        binding.menuButton.setOnClickListener {
            viewModel.post.value?.let { post ->
                showPostMenu(post)
            }
        }

        binding.attachmentContainer.setOnClickListener {
            viewModel.post.value?.attachment?.url?.let { url ->
                openInBrowser(url)
            }
        }

        binding.linkContainer.setOnClickListener {
            viewModel.post.value?.link?.let { link ->
                openInBrowser(link)
            }
        }
    }

    private fun loadPost() {
        val postId = arguments?.getLong(ARG_POST_ID) ?: 0L
        if (postId != 0L) {
            lifecycleScope.launch {
                viewModel.loadPost(postId)
            }
        } else {
            findNavController().popBackStack()
        }
    }

    private fun updatePostInfo(post: Post) {
        binding.toolbar.title = "Пост от ${post.author}"
        if (!post.authorAvatar.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(post.authorAvatar)
                .circleCrop()
                .placeholder(R.drawable.author_avatar)
                .into(binding.authorAvatar)
        } else {
            binding.authorAvatar.setImageResource(R.drawable.author_avatar)
        }
        binding.authorName.text = post.author
        binding.publishedTime.text = post.formattedDate
        binding.authorJob.text = post.authorJob ?: getString(R.string.looking_for_job)
        binding.content.text = post.content
        binding.likeCount.text = post.likeOwnerIds.size.toString()
        val likeIcon = if (post.likedByMe) {
            R.drawable.ic_like_filled_24
        } else {
            R.drawable.ic_like_24
        }
        binding.likeButton.setImageResource(likeIcon)

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

        val hasLink = !post.link.isNullOrEmpty()
        binding.linkContainer.isVisible = hasLink

        if (hasLink) {
            binding.linkText.text = post.link
        }
        val hasCoords = post.coords != null
        binding.mapContainer.isVisible = hasCoords

        if (hasCoords && !isMapInitialized) {
            post.coords?.let { coords ->
                showMap(coords)
                isMapInitialized = true
            }
        }
        binding.menuButton.isVisible = post.ownedByMe
    }

    private fun showMap(coords: Coordinates) {
        try {
            MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)
            MapKitFactory.initialize(requireContext())

            val mapView = binding.mapView
            val map = mapView.mapWindow.map
            val point = Point(coords.lat, coords.long)
            val mapObjects = map.mapObjects.addCollection()
            mapObjects.addPlacemark().apply {
                geometry = point
                setIcon(
                    ImageProvider.fromResource(requireContext(), R.drawable.ic_map_pin)
                )
                opacity = 1.0f
            }
            map.move(
                CameraPosition(point, 15.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 0f),
                null
            )

            mapView.mapWindow.map.isScrollGesturesEnabled = false
            mapView.mapWindow.map.isZoomGesturesEnabled = false
            mapView.mapWindow.map.isRotateGesturesEnabled = false
            mapView.mapWindow.map.isTiltGesturesEnabled = false

            binding.coordsText.text = String.format("%.6f, %.6f", coords.lat, coords.long)
            MapKitFactory.getInstance().onStart()
            mapView.onStart()

        } catch (e: Exception) {
            binding.mapContainer.visibility = View.GONE
            Log.e("PostDetailFragment", "Ошибка инициализации карты", e)
        }
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.cannot_open_link,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showPostMenu(post: Post) {
        val options = arrayOf(
            getString(R.string.edit_post),
            getString(R.string.delete_post)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.post_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToEditPost(post.id)
                    1 -> deletePost(post.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun navigateToEditPost(postId: Long) {
        Snackbar.make(binding.root, "Редактирование поста $postId", Snackbar.LENGTH_SHORT).show()
    }

    private fun deletePost(postId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_post)
            .setMessage(R.string.delete_post_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deletePost(postId)
                findNavController().popBackStack()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (binding.mapContainer.visibility == View.VISIBLE && isMapInitialized) {
            MapKitFactory.getInstance().onStart()
            binding.mapView.onStart()
        }
    }

    override fun onStop() {
        if (binding.mapContainer.visibility == View.VISIBLE && isMapInitialized) {
            binding.mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
        super.onStop()
    }

    override fun onDestroyView() {
        if (isMapInitialized) {
            binding.mapView.mapWindow.map.mapObjects.clear()
        }
        super.onDestroyView()
        _binding = null
    }
}