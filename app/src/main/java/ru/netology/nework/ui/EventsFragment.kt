package ru.netology.nework.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.EventsAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.dto.Event
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants.ARG_EVENT_ID
import ru.netology.nework.viewmodel.EventsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel by viewModels<EventsViewModel>()
    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        EventsAdapter(
            onLikeClickListener = { event ->
                viewModel.likeById(event.id)
            },
            onItemClickListener = { event ->
                findNavController().navigate(
                    R.id.action_eventsFragment_to_eventDetailFragment,
                    Bundle().apply {
                        putLong(ARG_EVENT_ID, event.id)
                    }
                )
            },
            onMenuClickListener = { event ->
                showEventMenu(event)
            },
            onAttachmentClickListener = { url ->
                openInBrowser(url)
            },
            onLinkClickListener = { url ->
                openInBrowser(url)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.eventsList.layoutManager = LinearLayoutManager(requireContext())
        binding.eventsList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.events)

            binding.emptyContainer.isVisible = state.empty
            binding.emptyTitle.isVisible = state.empty
            binding.emptySubtitle.isVisible = state.empty
            binding.retryButton.isVisible = state.empty
            binding.eventsList.isVisible = state.events.isNotEmpty()

            binding.progressBar.isVisible = state.loading && !state.refreshing
            binding.swipeRefresh.isRefreshing = state.refreshing

            binding.eventsList.isEnabled = !state.loading
            binding.fab.isEnabled = !state.loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshEvents()
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadEvents()
        }

        binding.fab.setOnClickListener {
            val isAuthorized = appAuth.authState.value?.id != 0L
            if (isAuthorized) {
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            } else {
                showLoginRequiredDialog()
            }
        }
    }

    private fun showEventMenu(event: Event) {
        if (!event.ownedByMe) return
        val options = arrayOf(
            getString(R.string.edit),
            getString(R.string.delete)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.event_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToEditEvent(event.id)
                    1 -> confirmDeleteEvent(event.id)
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

    private fun confirmDeleteEvent(eventId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_event)
            .setMessage(R.string.delete_event_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.removeById(eventId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

    private fun showError(error: AppError) {
        val message = when (error) {
            is AppError.ApiError -> error.message ?: "Ошибка сервера"
            is AppError.NetworkError -> "Нет соединения с сетью"
            else -> error.message ?: "Неизвестная ошибка"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) {
                viewModel.loadEvents()
            }
            .show()
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.cannot_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}