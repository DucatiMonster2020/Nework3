package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
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
                // TODO: переход к профилю пользователя
                // Пока просто показываем Snackbar
                Snackbar.make(binding.root, "Пользователь: ${user.name}", Snackbar.LENGTH_SHORT).show()
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
        loadUsers()
    }

    private fun setupRecyclerView() {
        binding.usersList.layoutManager = LinearLayoutManager(requireContext())
        binding.usersList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)

            // Простое сообщение если нет пользователей
            if (users.isEmpty()) {
                binding.usersList.isVisible = false
                // Можно показать простой TextView
            } else {
                binding.usersList.isVisible = true
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing

            if (state.error && state.errorMessage != null) {
                showError(state.errorMessage)
            }
        }
    }

    private fun setupListeners() {
        // Обновление по свайпу
        binding.swipeRefresh.setOnRefreshListener {
            refreshUsers()
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            viewModel.loadUsers()
        }
    }

    private fun refreshUsers() {
        lifecycleScope.launch {
            viewModel.refreshUsers()
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) { loadUsers() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}