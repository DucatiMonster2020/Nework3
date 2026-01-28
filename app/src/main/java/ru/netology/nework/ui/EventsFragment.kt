package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
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
                showSnackbar("Clicked event: ${event.id}")
                // TODO: переход к деталям события
            },
            onMenuClickListener = { event ->
                showEventMenu(event)
            },
            onParticipateClickListener = { event ->
                viewModel.participateById(event.id)
            },
            onAttachmentClickListener = { url ->
                showSnackbar("Attachment: $url")
                // TODO: открыть вложение
            },
            onLinkClickListener = { url ->
                showSnackbar("Link: $url")
                // TODO: открыть в браузере
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

            // Показываем/скрываем сообщение о пустом списке
            binding.emptyContainer.isVisible = state.empty
            binding.emptyTitle.isVisible = state.empty
            binding.emptySubtitle.isVisible = state.empty
            binding.retryButton.isVisible = state.empty
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            // Состояние загрузки
            binding.progressBar.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing

            // Ошибка
            if (state.error) {
                showError(state.errorMessage)
            }

            // Блокируем/разблокируем UI
            binding.eventsList.isEnabled = !state.loading
            binding.fab.isEnabled = !state.loading
        }
    }

    private fun setupListeners() {
        // Обновление по свайпу
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshEvents()
        }

        // Кнопка Retry
        binding.retryButton.setOnClickListener {
            viewModel.loadEvents()
        }

        // FAB для создания события
        binding.fab.setOnClickListener {
            val isAuthorized = AuthStateManager.authState.value is AuthStateManager.AuthState.Authorized
            if (isAuthorized) {
                showSnackbar("Create new event")
            } else {
                showLoginRequiredDialog()
            }
        }
    }
    private fun showLoginRequiredDialog() {
        showSnackbar("Please sign in to create new event")
    }

    private fun showEventMenu(event: ru.netology.nework.dto.Event) {
        // TODO: показать диалог с опциями удалить/редактировать
        showSnackbar("Menu for event ${event.id}")
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