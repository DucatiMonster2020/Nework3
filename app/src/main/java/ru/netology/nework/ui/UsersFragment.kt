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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.FragmentUsersBinding
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants.ARG_IS_CURRENT_USER
import ru.netology.nework.utils.Constants.ARG_USER_ID
import ru.netology.nework.viewmodel.UsersViewModel

@AndroidEntryPoint
class UsersFragment : Fragment() {

    private val viewModel by viewModels<UsersViewModel>()
    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                findNavController().navigate(
                    R.id.action_usersFragment_to_userDetailFragment,
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
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.users)
            binding.usersList.isVisible = state.users.isNotEmpty()
            binding.progressBar.isVisible = state.loading && !state.refreshing
            binding.swipeRefresh.isRefreshing = state.refreshing
            if (state.error != null) {
                showError(state.error)
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshUsers()
        }
    }

    private fun loadUsers() {
        viewModel.loadUsers()
    }

    private fun refreshUsers() {
        viewModel.refreshUsers()
    }

    private fun showError(error: AppError) {
        val message = when (error) {
            is AppError.ApiError -> error.message ?: "Ошибка загрузки"
            is AppError.NetworkError -> "Нет соединения с сетью"
            else -> error.message ?: "Неизвестная ошибка"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) { loadUsers() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}