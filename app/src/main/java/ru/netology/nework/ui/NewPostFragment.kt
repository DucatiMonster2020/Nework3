package ru.netology.nework.ui

import android.net.Uri
import android.os.Bundle
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.viewmodel.NewPostViewModel

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    private val viewModel by viewModels<NewPostViewModel>()
    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var selectedAttachmentUri: Uri? = null
    private var attachmentType: String? = null // "audio", "video", "image"

    // Для выбора изображения
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedAttachmentUri = it
            attachmentType = "image"
            binding.attachmentType.text = "Изображение выбрано"
            binding.removeAttachmentButton.isVisible = true
        }
    }

    // Для выбора видео
    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAttachmentUri = it
            attachmentType = "video"
            binding.attachmentType.text = "Видео выбрано"
            binding.removeAttachmentButton.isVisible = true
        }
    }

    // Для выбора аудио
    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAttachmentUri = it
            attachmentType = "audio"
            binding.attachmentType.text = "Аудио выбрано"
            binding.removeAttachmentButton.isVisible = true
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

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        // Кнопка выбора локации (пока заглушка)
        binding.locationButton.setOnClickListener {
            Snackbar.make(binding.root, "Выбор локации", Snackbar.LENGTH_SHORT).show()
        }

        // Кнопка выбора упомянутых пользователей (пока заглушка)
        binding.mentionsButton.setOnClickListener {
            Snackbar.make(binding.root, "Выбор пользователей", Snackbar.LENGTH_SHORT).show()
        }

        // Кнопки выбора изображения
        binding.photoButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.galleryButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Кнопки выбора вложения
        binding.audioButton.setOnClickListener {
            pickAudioLauncher.launch("audio/*")
        }

        binding.videoButton.setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        // Кнопка удаления вложения
        binding.removeAttachmentButton.setOnClickListener {
            selectedAttachmentUri = null
            attachmentType = null
            binding.attachmentType.text = "Вложение не выбрано"
            binding.removeAttachmentButton.isVisible = false
        }

        // Кнопка выбора ссылки
        binding.linkButton.setOnClickListener {
            // Простое поле для ввода ссылки
            binding.linkInput.isVisible = !binding.linkInput.isVisible
        }
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
                // Возвращаемся назад (по ТЗ: "возврат назад к списку постов")
                findNavController().popBackStack()
            }
        }
    }

    private fun savePost() {
        val content = binding.content.text.toString().trim()

        if (content.isEmpty()) {
            binding.content.error = "Текст поста не может быть пустым"
            return
        }

        val link = if (binding.linkInput.isVisible) {
            binding.linkInput.editText?.text.toString().trim()
        } else {
            null
        }

        // Проверка размера вложения (15 МБ по ТЗ)
        // Пока пропускаем - в реальном проекте нужно проверить

        lifecycleScope.launch {
            viewModel.savePost(
                content = content,
                link = link,
                attachmentUri = selectedAttachmentUri,
                attachmentType = attachmentType
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}