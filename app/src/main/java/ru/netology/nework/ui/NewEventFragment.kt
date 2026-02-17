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
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.User
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants.ARG_EVENT_ID
import ru.netology.nework.utils.Constants.LOCATION_LAT
import ru.netology.nework.utils.Constants.LOCATION_LNG
import ru.netology.nework.utils.Constants.LOCATION_REQUEST_KEY
import ru.netology.nework.utils.CoordinatesUtils.formatCoordinates
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
    private var selectedCoords: Coordinates? = null
    private var currentEventId = 0L
    private var isEditMode = false
    private val selectedSpeakers = mutableListOf<User>()

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

        arguments?.let {
            currentEventId = it.getLong(ARG_EVENT_ID, 0L)
            isEditMode = currentEventId != 0L
        }
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
        setupFragmentResultListener()
    }

    private fun setupListeners() {
        binding.locationButton.setOnClickListener {
            findNavController().navigate(R.id.action_newEventFragment_to_mapFragment)
        }

        binding.eventTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.onlineRadio -> {
                    binding.locationButton.isVisible = false
                }
                R.id.offlineRadio -> {
                    binding.locationButton.isVisible = true
                }
            }
        }

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
            navigateToUsersSelection()
        }

        binding.removeAttachmentButton.setOnClickListener {
            clearAttachment()
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
            error?.let { showError(it) }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().popBackStack()
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
    }

    private fun navigateToUsersSelection() {
        val dialog = UsersSelectionDialogFragment.newInstance(
            selectedSpeakers.map { it.id }.toLongArray()
        )
        dialog.show(parentFragmentManager, "speakers_selection")
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

    private fun updateLocationButton(coords: Coordinates) {
        binding.locationButton.apply {
            text = "Место: ${formatCoordinates(coords)}"
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_check, 0, 0, 0
            )
        }
    }

    private fun clearAttachment() {
        selectedImageUri = null
        selectedAttachmentUri = null
        attachmentType = null
        binding.attachmentType.text = "Вложение не выбрано"
        binding.removeAttachmentButton.isVisible = false
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
        // Исправлено: используем правильный ID - linkInput
        val link = if (binding.linkInput.isVisible) {
            binding.linkInput.editText?.text?.toString()?.trim()
        } else {
            null
        }

        lifecycleScope.launch {
            viewModel.saveEvent(
                content = content,
                datetime = eventDate!!,
                isOnline = isOnline,
                link = link,
                coords = if (!isOnline) selectedCoords else null,
                speakerIds = selectedSpeakers.map { it.id }
            )
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