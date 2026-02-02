package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import ru.netology.nework.auth.AuthStateManager
import ru.netology.nework.databinding.FragmentEventsBinding
import ru.netology.nework.viewmodel.EventsViewModel

@AndroidEntryPoint
class EventsFragment : Fragment() {

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
                        putLong("eventId", event.id)
                    }
                )
            },
            onMenuClickListener = { event ->
                showEventMenu(event)
            },
            onAttachmentClickListener = { url ->
                showSnackbar("Attachment: $url")
            },
            onLinkClickListener = { url ->
                showSnackbar("Link: $url")
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
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing
            if (state.error) {
                showError(state.errorMessage)
            }
            binding.eventsList.isEnabled = !state.loading
            binding.fab.isEnabled = !state.loading
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
            val isAuthorized = AuthStateManager.authState.value is AuthStateManager.AuthState.Authorized
            if (isAuthorized) {
                findNavController().navigate(R.id.action_eventsFragment_to_newEventFragment)
            } else {
                showLoginRequiredDialog()
            }
        }
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

    private fun showEventMenu(event: ru.netology.nework.dto.Event) {
        Snackbar.make(binding.root, "Menu for event ${event.id}", Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String?) {
        val errorMsg = message ?: "An error occurred"
        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                viewModel.loadEvents()
            }
            .show()
    }
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}