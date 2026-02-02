package ru.netology.nework.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
            if (validateImageFile(it)) {
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
    private val pickUsersLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongArrayExtra("selectedUserIds")?.let { userIds ->
                loadSelectedUsers(userIds.toList())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        setupToolbar()
        setupSelectedUsersList()
        setupObservers()
        setupListeners()
        setupFragmentResultListener()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
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
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().popBackStack()
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
            Snackbar.make(binding.root, "Фотоаппарат", Snackbar.LENGTH_SHORT).show()
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
            val isVisible = binding.linkInput.isVisible
            binding.linkInput.isVisible = !isVisible
            if (!isVisible) {
                binding.linkEditText.requestFocus()
            }
        }
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener(
            "location_request_key",
            viewLifecycleOwner
        ) { requestKey, result ->
            if (requestKey == "location_request_key") {
                val lat = result.getDouble("lat")
                val lng = result.getDouble("lng")

                if (lat != 0.0 && lng != 0.0) {
                    selectedCoords = Coordinates(lat, lng)
                    updateLocationButton(lat, lng)
                }
            }
        }
    }

    private fun navigateToUsersSelection() {
        val intent = Intent(requireContext(), UsersSelectionActivity::class.java).apply {
            putExtra("selectedUserIds", selectedUsers.map { it.id }.toLongArray())
        }
        pickUsersLauncher.launch(intent)
    }

    private fun loadSelectedUsers(userIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = viewModel.loadUsersByIds(userIds)
                selectedUsers.clear()
                selectedUsers.addAll(response)
                updateSelectedUsersList()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка загрузки пользователей", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedUsersList() {
        selectedUsersAdapter.submitList(selectedUsers.toList())
        binding.selectedUsersList.isVisible = selectedUsers.isNotEmpty()
    }

    private fun updateLocationButton(lat: Double, lng: Double) {
        binding.locationButton.apply {
            text = String.format("Место: %.4f, %.4f", lat, lng)
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_check, 0, 0, 0
            )
            compoundDrawablePadding = 8
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
    private fun validateImageFile(uri: Uri): Boolean {
        return try {
            val fileSize = FileUtils.getFileSize(uri, requireContext())
            if (fileSize > 15 * 1024 * 1024) {
                showFileSizeError(fileSize)
                return false
            }
            val mimeType = requireContext().contentResolver.getType(uri)
            val isValidFormat = mimeType in arrayOf("image/jpeg", "image/png")

            if (!isValidFormat) {
                Snackbar.make(
                    binding.root,
                    "Поддерживаются только JPG и PNG",
                    Snackbar.LENGTH_SHORT
                ).show()
                return false
            }

            true
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Ошибка проверки файла", Snackbar.LENGTH_SHORT).show()
            false
        }
    }

    private fun validateMediaFile(uri: Uri, expectedType: String): Boolean {
        return try {
            val fileSize = FileUtils.getFileSize(uri, requireContext())
            if (fileSize > 15 * 1024 * 1024) {
                showFileSizeError(fileSize)
                return false
            }
            val mimeType = requireContext().contentResolver.getType(uri)
            val isValidType = mimeType?.startsWith(expectedType.substringBefore("/*")) == true

            if (!isValidType) {
                Snackbar.make(
                    binding.root,
                    "Неподдерживаемый формат файла",
                    Snackbar.LENGTH_SHORT
                ).show()
                return false
            }

            true
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Ошибка проверки файла", Snackbar.LENGTH_SHORT).show()
            false
        }
    }

    private fun showFileSizeError(fileSize: Long) {
        val formattedSize = FileUtils.formatFileSize(fileSize)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Файл слишком большой")
            .setMessage("Размер файла: $formattedSize\nМаксимальный размер: 15 МБ")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun savePost() {
        val content = binding.content.text.toString().trim()

        if (content.isEmpty()) {
            binding.content.error = getString(R.string.content_can_not_be_empty)
            return
        }

        val link = if (binding.linkInput.isVisible) {
            binding.linkEditText.text.toString().trim()
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
            selectedAttachmentUri?.let { uri ->
                try {
                    mediaUrl = viewModel.uploadMedia(uri, attachmentType ?: AttachmentType.IMAGE)
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Ошибка загрузки вложения", Snackbar.LENGTH_SHORT).show()
                    return@launch
                }
            }
            val attachment = if (mediaUrl != null && attachmentType != null) {
                Attachment(mediaUrl!!, attachmentType!!)
            } else {
                null
            }
            viewModel.savePost(
                content = content,
                link = link,
                coords = selectedCoords,
                mentionIds = mentionIds,
                attachment = attachment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}