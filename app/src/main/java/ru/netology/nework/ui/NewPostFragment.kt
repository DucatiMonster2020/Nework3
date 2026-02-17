package ru.netology.nework.ui

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.User
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants.ARG_POST_ID
import ru.netology.nework.utils.Constants.LOCATION_LAT
import ru.netology.nework.utils.Constants.LOCATION_LNG
import ru.netology.nework.utils.Constants.LOCATION_REQUEST_KEY
import ru.netology.nework.utils.CoordinatesUtils.formatCoordinates
import ru.netology.nework.utils.FileUtils
import ru.netology.nework.viewmodel.NewPostViewModel

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    private val viewModel by viewModels<NewPostViewModel>()
    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var selectedAttachmentUri: Uri? = null
    private var attachmentType: AttachmentType? = null
    private var selectedCoords: Coordinates? = null
    private val selectedUsers = mutableListOf<User>()
    private var isEditMode = false
    private var currentPostId = 0L

    private val selectedUsersAdapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                selectedUsers.remove(user)
                updateSelectedUsersList()
            }
        )
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (validateMediaFile(it, "image/*")) {
                selectedImageUri = it
                selectedAttachmentUri = it
                attachmentType = AttachmentType.IMAGE
                updateAttachmentInfo("Изображение")
            }
        }
    }

    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (validateMediaFile(it, "video/*")) {
                selectedAttachmentUri = it
                attachmentType = AttachmentType.VIDEO
                updateAttachmentInfo("Видео")
            }
        }
    }

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (validateMediaFile(it, "audio/*")) {
                selectedAttachmentUri = it
                attachmentType = AttachmentType.AUDIO
                updateAttachmentInfo("Аудио")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.let {
            currentPostId = it.getLong(ARG_POST_ID, 0L)
            isEditMode = currentPostId != 0L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.new_post_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                savePost()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSelectedUsersList()
        setupObservers()
        setupListeners()
        setupFragmentResultListener()

        if (isEditMode) {
            loadPostForEditing(currentPostId)
        }
    }

    private fun setupSelectedUsersList() {
        binding.selectedUsersList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.selectedUsersList.adapter = selectedUsersAdapter
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.content.isEnabled = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigate(R.id.action_newPostFragment_to_postsFragment)
            }
        }
    }

    private fun setupListeners() {
        binding.locationButton.setOnClickListener {
            findNavController().navigate(R.id.action_newPostFragment_to_mapFragment)
        }

        binding.mentionsButton.setOnClickListener {
            navigateToUsersSelection()
        }

        binding.photoButton.setOnClickListener {
            Toast.makeText(requireContext(), "Камера будет доступна в следующей версии", Toast.LENGTH_SHORT).show()
        }

        binding.galleryButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.audioButton.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        binding.videoButton.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        binding.removeAttachmentButton.setOnClickListener {
            clearAttachment()
        }

        binding.linkButton.setOnClickListener {
            binding.linkInput.isVisible = !binding.linkInput.isVisible
            if (!binding.linkInput.isVisible) {
                binding.linkEditText.text?.clear()
            } else {
                binding.linkEditText.requestFocus()
            }
        }
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener(
            LOCATION_REQUEST_KEY,
            viewLifecycleOwner
        ) { requestKey, result ->
            if (requestKey == LOCATION_REQUEST_KEY) {
                val lat = result.getDouble(LOCATION_LAT)
                val lng = result.getDouble(LOCATION_LNG)

                if (lat != 0.0 && lng != 0.0) {
                    selectedCoords = Coordinates(lat, lng)
                    updateLocationButton(selectedCoords!!)
                }
            }
        }

        parentFragmentManager.setFragmentResultListener(
            UsersSelectionDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { requestKey, result ->
            if (requestKey == UsersSelectionDialogFragment.REQUEST_KEY) {
                result.getLongArray(UsersSelectionDialogFragment.RESULT_SELECTED_IDS)?.let { userIds ->
                    loadSelectedUsers(userIds.toList())
                }
            }
        }
    }

    private fun navigateToUsersSelection() {
        val dialog = UsersSelectionDialogFragment.newInstance(
            selectedUsers.map { it.id }.toLongArray()
        )
        dialog.show(parentFragmentManager, "users_selection")
    }

    private fun loadSelectedUsers(userIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val users = viewModel.loadUsersByIds(userIds)
                selectedUsers.clear()
                selectedUsers.addAll(users)
                updateSelectedUsersList()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка загрузки пользователей", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedUsersList() {
        selectedUsersAdapter.submitList(selectedUsers.toList())
        binding.selectedUsersList.isVisible = selectedUsers.isNotEmpty()
        binding.selectedUsersTitle.isVisible = selectedUsers.isNotEmpty()
    }

    private fun updateLocationButton(coords: Coordinates) {
        binding.locationButton.apply {
            text = "Место: ${formatCoordinates(coords)}"
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_check, 0, 0, 0
            )
        }
    }

    private fun updateAttachmentInfo(type: String) {
        binding.attachmentType.text = "Вложение: $type"
        binding.removeAttachmentButton.isVisible = true
    }

    private fun clearAttachment() {
        selectedImageUri = null
        selectedAttachmentUri = null
        attachmentType = null
        binding.attachmentType.text = getString(R.string.no_attachment)
        binding.removeAttachmentButton.isVisible = false
    }

    private fun validateMediaFile(uri: Uri, expectedType: String): Boolean {
        if (!FileUtils.isFileSizeValid(uri, requireContext())) {
            showFileSizeError(FileUtils.getFileSize(uri, requireContext()))
            return false
        }

        val isValidType = when (expectedType) {
            "image/*" -> FileUtils.isImageFile(uri, requireContext())
            "video/*" -> FileUtils.isVideoFile(uri, requireContext())
            "audio/*" -> FileUtils.isAudioFile(uri, requireContext())
            else -> false
        }

        if (!isValidType) {
            Snackbar.make(
                binding.root,
                "Неподдерживаемый формат файла",
                Snackbar.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun showFileSizeError(fileSize: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Файл слишком большой")
            .setMessage("Размер файла: ${FileUtils.formatFileSize(fileSize)}\nМаксимальный размер: 15 МБ")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadPostForEditing(postId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val post = viewModel.loadPostForEditing(postId)
                post?.let {
                    binding.content.setText(it.content)

                    if (!it.link.isNullOrEmpty()) {
                        binding.linkInput.isVisible = true
                        binding.linkEditText.setText(it.link)
                    }

                    it.coords?.let { coords ->
                        selectedCoords = coords
                        updateLocationButton(coords)
                    }

                    if (it.mentionIds.isNotEmpty()) {
                        loadSelectedUsers(it.mentionIds)
                    }

                    it.attachment?.let { attachment ->
                        attachmentType = attachment.type
                        updateAttachmentInfo(attachment.type.name.lowercase())
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка загрузки поста", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePost() {
        val content = binding.content.text.toString().trim()

        if (content.isEmpty()) {
            binding.content.error = getString(R.string.content_can_not_be_empty)
            return
        }

        val link = if (binding.linkInput.isVisible) {
            binding.linkEditText.text.toString().trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }

        if (!link.isNullOrEmpty() && !Patterns.WEB_URL.matcher(link).matches()) {
            binding.linkEditText.error = "Некорректная ссылка"
            return
        }

        val mentionIds = selectedUsers.map { it.id }

        viewLifecycleOwner.lifecycleScope.launch {
            var mediaUrl: String? = null

            if (selectedAttachmentUri != null && attachmentType != null) {
                try {
                    mediaUrl = viewModel.uploadMedia(
                        requireContext(),
                        selectedAttachmentUri!!,
                        attachmentType!!
                    )
                } catch (e: Exception) {
                    showError(AppError.fromThrowable(e))
                    return@launch
                }
            }

            val attachment = if (mediaUrl != null && attachmentType != null) {
                Attachment(mediaUrl!!, attachmentType!!)
            } else {
                null
            }

            if (isEditMode) {
                viewModel.updatePost(
                    postId = currentPostId,
                    content = content,
                    link = link,
                    coords = selectedCoords,
                    mentionIds = mentionIds,
                    attachment = attachment
                )
            } else {
                viewModel.savePost(
                    content = content,
                    link = link,
                    coords = selectedCoords,
                    mentionIds = mentionIds,
                    attachment = attachment
                )
            }
        }
    }

    private fun showError(error: AppError) {
        val message = when (error) {
            is AppError.ApiError -> error.message ?: "Ошибка сервера"
            is AppError.NetworkError -> "Нет соединения с сетью"
            is AppError.ValidationError -> "Ошибка валидации"
            else -> error.message ?: "Неизвестная ошибка"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}