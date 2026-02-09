package ru.netology.nework.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentEventDetailBinding
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.NotFoundError
import ru.netology.nework.utils.Constants.ARG_EVENT_ID
import ru.netology.nework.utils.Constants.ARG_IS_CURRENT_USER
import ru.netology.nework.utils.Constants.ARG_USER_ID
import ru.netology.nework.viewmodel.EventDetailViewModel

@AndroidEntryPoint
class EventDetailFragment : Fragment() {

    companion object {
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
                findNavController().navigate(
                    R.id.action_global_userDetailFragment,
                    Bundle().apply {
                        putLong(ARG_USER_ID, user.id)
                        putBoolean(ARG_IS_CURRENT_USER, false)
                    }
                )
            }
        )
    }
    private val participantsAdapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                findNavController().navigate(
                    R.id.action_global_userDetailFragment,
                    Bundle().apply {
                        putLong(ARG_USER_ID, user.id)
                        putBoolean(ARG_IS_CURRENT_USER, false)
                    }
                )
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
        binding.speakersList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.speakersList.adapter = speakersAdapter
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
            binding.speakersTitle.isVisible = speakers.isNotEmpty()
            binding.speakersList.isVisible = speakers.isNotEmpty()
        }
        viewModel.participants.observe(viewLifecycleOwner) { participants ->
            participantsAdapter.submitList(participants)
            binding.participantsTitle.isVisible = participants.isNotEmpty()
            binding.participantsList.isVisible = participants.isNotEmpty()
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.scrollView.isVisible = !loading
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                val message = when (it) {
                    is ApiError -> it.message ?: "Ошибка API"
                    is NetworkError -> "Нет соединения с сетью"
                    is NotFoundError -> "Событие не найдено"
                    else -> "Неизвестная ошибка"
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { loadEvent() }
                    .show()
            }
        }
    }
    private fun setupListeners() {
        binding.likeButton.setOnClickListener {
            viewModel.event.value?.let { event ->
                viewModel.likeEvent(event.id)
            }
        }
        binding.participateButton.setOnClickListener {
            viewModel.event.value?.let { event ->
                viewModel.participateEvent(event.id)
            }
        }
        binding.optionsButton.setOnClickListener {
            viewModel.event.value?.let { event ->
                showEventMenu(event)
            }
        }
        binding.attachmentContainer.setOnClickListener {
            viewModel.event.value?.attachment?.url?.let { url ->
                openInBrowser(url)
            }
        }
        binding.linkContainer.setOnClickListener {
            viewModel.event.value?.link?.let { link ->
                openInBrowser(link)
            }
        }
    }
    private fun loadEvent() {
        val eventId = arguments?.getLong(ARG_EVENT_ID) ?: 0L
        if (eventId != 0L) {
            lifecycleScope.launch {
                viewModel.loadEvent(eventId)
            }
        } else {
            findNavController().popBackStack()
        }
    }
    private fun updateEventInfo(event: Event) {
        if (!event.authorAvatar.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(event.authorAvatar)
                .circleCrop()
                .placeholder(R.drawable.author_avatar)
                .into(binding.authorAvatar)
        } else {
            binding.authorAvatar.setImageResource(R.drawable.author_avatar)
        }
        binding.authorName.text = event.author
        binding.published.text = event.formattedPublished
        binding.eventDateTime.text = event.formattedDateTime
        binding.eventType.text = when (event.type) {
            EventType.ONLINE -> "ONLINE"
            EventType.OFFLINE -> "OFFLINE"
        }
        val typeColor = when (event.type) {
            EventType.ONLINE -> R.color.event_online
            EventType.OFFLINE -> R.color.event_offline
        }
        binding.eventType.setBackgroundColor(
            ContextCompat.getColor(requireContext(), typeColor)
        )
        binding.content.text = event.content
        binding.likeCount.text = event.likeOwnerIds.size.toString()
        binding.likeButton.setImageResource(
            if (event.likedByMe) R.drawable.ic_like_filled_24 else R.drawable.ic_like_24
        )
        val hasAttachment = event.attachment != null
        binding.attachmentContainer.isVisible = hasAttachment
        if (hasAttachment) {
            event.attachment?.let { attachment ->
                binding.attachmentType.text = when (attachment.type) {
                    AttachmentType.IMAGE -> "Фото"
                    AttachmentType.VIDEO -> "Видео"
                    AttachmentType.AUDIO -> "Аудио"
                }
                binding.attachmentUrl.text = attachment.url
            }
        }
        val hasLink = !event.link.isNullOrEmpty()
        binding.linkContainer.isVisible = hasLink
        if (hasLink) {
            binding.linkText.text = event.link
        }
        binding.authorJob.text = event.authorJob ?: getString(R.string.looking_for_job)
        val shouldShowMap = event.type == EventType.OFFLINE && event.coords != null
        binding.mapContainer.isVisible = shouldShowMap
        if (shouldShowMap) {
            event.coords?.let { coords ->
                showMap(coords)
            }
        }
        binding.optionsButton.isVisible = event.ownedByMe
    }
    private fun showMap(coords: Coordinates) {
        try {
            MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)
            MapKitFactory.initialize(requireContext())

            val mapView = binding.mapView
            val map = mapView.map
            val point = Point(coords.lat, coords.long)
            map.mapObjects.addPlacemark(point).apply {
                setIcon(
                    com.yandex.runtime.image.ImageProvider.fromResource(
                        requireContext(), R.drawable.ic_map_pin
                    )
                )
            }
            map.move(
                CameraPosition(point, 15.0f, 0.0f, 0.0f),
                com.yandex.mapkit.Animation(com.yandex.mapkit.Animation.Type.SMOOTH, 0.5f),
                null
            )
            binding.coordsText.text = String.format("%.6f, %.6f", coords.lat, coords.long)

            MapKitFactory.getInstance().onStart()
            mapView.onStart()

        } catch (e: Exception) {
            binding.mapContainer.isVisible = false
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
    private fun showEventMenu(event: Event) {
        val options = arrayOf(
            getString(R.string.edit),
            getString(R.string.delete)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.event_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToEditEvent(event.id)
                    1 -> deleteEvent(event.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun navigateToEditEvent(eventId: Long) {
        findNavController().navigate(
            R.id.newEventFragment,
            Bundle().apply {
                putLong(ARG_EVENT_ID, eventId)
            }
        )
    }
    private fun deleteEvent(eventId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_event)
            .setMessage(R.string.delete_event_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteEvent(eventId)
                findNavController().popBackStack()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (binding.mapContainer.isVisible) {
            MapKitFactory.getInstance().onStart()
            binding.mapView.onStart()
        }
    }

    override fun onStop() {
        if (binding.mapContainer.isVisible) {
            binding.mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
        super.onStop()
    }

    override fun onDestroyView() {
        if (binding.mapContainer.isVisible) {
            binding.mapView.map.mapObjects.clear()
        }
        super.onDestroyView()
        _binding = null
    }
}