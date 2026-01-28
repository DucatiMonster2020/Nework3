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
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentUsersBinding
import ru.netology.nework.viewmodel.UsersViewModel

@AndroidEntryPoint
class UsersFragment : Fragment() {

    private val viewModel by viewModels<UsersViewModel>()
    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                showSnackbar("Clicked user: ${user.name}")
                // TODO: переход к профилю пользователя
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.usersList.layoutManager = LinearLayoutManager(requireContext())
        binding.usersList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)

            // Показываем/скрываем сообщение о пустом списке
            binding.emptyContainer.isVisible = users.isEmpty()
            binding.emptyTitle.isVisible = users.isEmpty()
            binding.emptySubtitle.isVisible = users.isEmpty()
            binding.retryButton.isVisible = users.isEmpty()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            // Состояние загрузки
            binding.progressBar.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing

            // Ошибка
            if (state.error) {
                showError(state.errorMessage)
            }
            binding.usersList.isEnabled = !state.loading
        }
    }

    private fun setupListeners() {
        // Обновление по свайпу
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshUsers()
        }

        // Кнопка Retry
        binding.retryButton.setOnClickListener {
            viewModel.loadUsers()
        }
    }

    private fun showError(message: String?) {
        val errorMsg = message ?: "An error occurred"
        Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                viewModel.loadUsers()
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