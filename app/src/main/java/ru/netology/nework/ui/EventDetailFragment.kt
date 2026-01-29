package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentEventDetailBinding
import ru.netology.nework.viewmodel.EventDetailViewModel

@AndroidEntryPoint
class EventDetailFragment : Fragment() {

    companion object {
        private const val ARG_EVENT_ID = "event_id"

        fun newInstance(eventId: Long): EventDetailFragment {
            return EventDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_EVENT_ID, eventId)
                }
            }
        }
    }

    private val viewModel by viewModels<EventDetailViewModel>()
    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!

    private val speakersAdapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                Snackbar.make(binding.root, "Спикер: ${user.name}", Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    private val participantsAdapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                Snackbar.make(binding.root, "Участник: ${user.name}", Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupLists()
        setupObservers()
        setupListeners()
        loadEvent()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupLists() {
        // Список спикеров
        binding.speakersList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.speakersList.adapter = speakersAdapter

        // Список участников
        binding.participantsList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.participantsList.adapter = participantsAdapter
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            event?.let {
                updateEventInfo(it)
            }
        }

        viewModel.speakers.observe(viewLifecycleOwner) { speakers ->
            speakersAdapter.submitList(speakers)
            binding.speakersList.isVisible = speakers.isNotEmpty()
        }

        viewModel.participants.observe(viewLifecycleOwner) { participants ->
            participantsAdapter.submitList(participants)
            binding.participantsList.isVisible = participants.isNotEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.contentContainer.isVisible = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { loadEvent() }
                    .show()
            }
        }
    }

    private fun setupListeners() {
        // Лайк
        binding.likeButton.setOnClickListener {
            viewModel.event.value?.let { event ->
                viewModel.likeEvent(event.id)
            }
        }

        // Участие
        binding.participateButton.setOnClickListener {
            viewModel.event.value?.let { event ->
                viewModel.participateEvent(event.id)
            }
        }

        // Меню (только для автора)
        binding.menuButton.setOnClickListener {
            showEventMenu()
        }

        // Вложение
        binding.attachmentContainer.setOnClickListener {
            viewModel.event.value?.attachment?.url?.let { url ->
                Snackbar.make(binding.root, "Открыть: $url", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Ссылка
        binding.linkContainer.setOnClickListener {
            viewModel.event.value?.link?.let { link ->
                Snackbar.make(binding.root, "Открыть ссылку: $link", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Карта
        binding.mapContainer.setOnClickListener {
            viewModel.event.value?.coords?.let { coords ->
                Snackbar.make(binding.root, "Координаты: ${coords.lat}, ${coords.long}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadEvent() {
        val eventId = arguments?.getLong(ARG_EVENT_ID) ?: 0L
        if (eventId != 0L) {
            lifecycleScope.launch {
                viewModel.loadEvent(eventId)
            }
        }
    }

    private fun updateEventInfo(event: ru.netology.nework.dto.Event) {
        // Заголовок
        binding.toolbar.title = "Событие от ${event.author}"

        // Аватар автора
        if (!event.authorAvatar.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(event.authorAvatar)
                .circleCrop()
                .placeholder(R.drawable.author_avatar)
                .into(binding.authorAvatar)
        }

        // Информация об авторе
        binding.authorName.text = event.author
        binding.publishedTime.text = event.formattedPublished

        // Последнее место работы (ТЗ)
        binding.authorJob.text = event.authorJob ?: getString(R.string.looking_for_job)
        binding.authorJob.isVisible = true

        // Тип события
        binding.eventType.text = when (event.type) {
            ru.netology.nework.enumeration.EventType.ONLINE -> "ONLINE"
            ru.netology.nework.enumeration.EventType.OFFLINE -> "OFFLINE"
        }

        // Дата и время проведения
        binding.eventDateTime.text = event.formattedDateTime

        // Контент
        binding.content.text = event.content

        // Лайки
        binding.likeCount.text = event.likeOwnerIds.size.toString()
        binding.likeButton.isChecked = event.likedByMe

        // Участие
        binding.participantsCount.text = event.participantsIds.size.toString()
        binding.participateButton.isChecked = event.participatedByMe

        // Вложение
        val hasAttachment = event.attachment != null
        binding.attachmentContainer.isVisible = hasAttachment

        if (hasAttachment) {
            event.attachment?.let { attachment ->
                binding.attachmentType.text = when (attachment.type) {
                    ru.netology.nework.enumeration.AttachmentType.IMAGE -> "Фото"
                    ru.netology.nework.enumeration.AttachmentType.VIDEO -> "Видео"
                    ru.netology.nework.enumeration.AttachmentType.AUDIO -> "Аудио"
                }
                binding.attachmentUrl.text = attachment.url
            }
        }

        // Ссылка
        val hasLink = !event.link.isNullOrEmpty()
        binding.linkContainer.isVisible = hasLink

        if (hasLink) {
            binding.linkText.text = event.link
        }

        // Карта (если есть координаты)
        val hasCoords = event.coords != null
        binding.mapContainer.isVisible = hasCoords

        if (hasCoords) {
            binding.coordsText.text = "Координаты: ${event.coords?.lat}, ${event.coords?.long}"
        }

        // Кнопка меню (только для автора)
        binding.menuButton.isVisible = event.ownedByMe
    }

    private fun showEventMenu() {
        Snackbar.make(binding.root, "Меню события", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}