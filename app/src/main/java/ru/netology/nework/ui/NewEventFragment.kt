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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentNewEventBinding
import ru.netology.nework.viewmodel.NewEventViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NewEventFragment : Fragment() {

    private val viewModel by viewModels<NewEventViewModel>()
    private var _binding: FragmentNewEventBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var selectedAttachmentUri: Uri? = null
    private var attachmentType: String? = null
    private var eventDate: Date? = null

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
        _binding = FragmentNewEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.new_event_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                saveEvent()
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
        binding.locationButton.setOnClickListener {
            Snackbar.make(binding.root, "Выбор локации", Snackbar.LENGTH_SHORT).show()
        }
        binding.eventTypeGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.onlineRadio -> {
                    binding.locationButton.isVisible = false
                }
                R.id.offlineRadio -> {
                    binding.locationButton.isVisible = true
                }
            }
        }
        val isOnline = binding.onlineRadio.isChecked
        binding.dateButton.setOnClickListener {
            showDateTimePicker()
        }
        binding.photoButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
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
        binding.speakersButton.setOnClickListener {
            Snackbar.make(binding.root, "Выбор спикеров", Snackbar.LENGTH_SHORT).show()
        }
        binding.removeAttachmentButton.setOnClickListener {
            selectedAttachmentUri = null
            attachmentType = null
            binding.attachmentType.text = "Вложение не выбрано"
            binding.removeAttachmentButton.isVisible = false
        }
        binding.linkButton.setOnClickListener {
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
                findNavController().popBackStack()
            }
        }
    }

    private fun showDateTimePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите дату")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText("Выберите время")
                .setHour(12)
                .setMinute(0)
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selection
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                }

                eventDate = calendar.time
                updateDateButton()
            }

            timePicker.show(parentFragmentManager, "time_picker")
        }

        datePicker.show(parentFragmentManager, "date_picker")
    }

    private fun updateDateButton() {
        eventDate?.let { date ->
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            binding.dateButton.text = formatter.format(date)
        }
    }
    private fun saveEvent() {
        val content = binding.content.text.toString().trim()

        if (content.isEmpty()) {
            binding.content.error = "Текст события не может быть пустым"
            return
        }

        if (eventDate == null) {
            Snackbar.make(binding.root, "Выберите дату проведения", Snackbar.LENGTH_SHORT).show()
            return
        }

        val isOnline = binding.onlineRadio.isChecked
        val link = if (binding.linkInput.isVisible) {
            binding.linkInput.editText?.text.toString().trim()
        } else {
            null
        }
        lifecycleScope.launch {
            viewModel.saveEvent(
                content = content,
                datetime = eventDate!!,
                isOnline = isOnline,
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