package ru.netology.nework.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import ru.netology.nework.databinding.FragmentEventDetailBinding
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.enumeration.AttachmentType
import ru.netology.nework.enumeration.EventType
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
                findNavController().navigate(
                    R.id.action_global_userDetailFragment,
                    Bundle().apply {
                        putLong("userId", user.id)
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

        binding.menuButton.setOnClickListener {
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
        binding.toolbar.title = "Событие от ${event.author}"
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
        binding.authorJob.text = event.authorJob ?: getString(R.string.looking_for_job)
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
        binding.eventDateTime.text = event.formattedDateTime
        binding.content.text = event.content
        binding.likeCount.text = event.likeOwnerIds.size.toString()
        val likeIcon = if (event.likedByMe) {
            R.drawable.ic_like_filled_24
        } else {
            R.drawable.ic_like_24
        }
        binding.likeButton.setImageResource(likeIcon)
        val participateIcon = if (event.participatedByMe) {
            R.drawable.ic_check_filled_24
        } else {
            R.drawable.ic_check
        }
        binding.participateButton.setImageResource(participateIcon)
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
        val shouldShowMap = event.type == EventType.OFFLINE && event.coords != null
        binding.mapContainer.isVisible = shouldShowMap

        if (shouldShowMap && !isMapInitialized) {
            event.coords?.let { coords ->
                showMap(coords)
                isMapInitialized = true
            }
        }
        binding.menuButton.isVisible = event.ownedByMe
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
            Log.e("EventDetailFragment", "Ошибка инициализации карты", e)
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
            .setTitle(R.string.event_menu)
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
        Snackbar.make(binding.root, "Редактирование события $eventId", Snackbar.LENGTH_SHORT).show()
    }

    private fun deleteEvent(eventId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage("Удалить это событие?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteEvent(eventId)
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