package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.UsersAdapter
import ru.netology.nework.databinding.DialogUsersSelectionBinding
import ru.netology.nework.error.AppError
import ru.netology.nework.viewmodel.UsersViewModel

@AndroidEntryPoint
class UsersSelectionDialogFragment : DialogFragment() {

    private var _binding: DialogUsersSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<UsersViewModel>()

    private val selectedUserIds = mutableSetOf<Long>()
    private val adapter: UsersAdapter by lazy {
        UsersAdapter(
            onItemClickListener = { user ->
                if (selectedUserIds.contains(user.id)) {
                    selectedUserIds.remove(user.id)
                } else {
                    selectedUserIds.add(user.id)
                }
                this@UsersSelectionDialogFragment.adapter.updateSelectedIds(selectedUserIds.toSet())
            },
            isSelectionMode = true,
            selectedIds = selectedUserIds.toSet()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_App_Dialog_FullScreen)

        arguments?.getLongArray(ARG_SELECTED_IDS)?.let { ids ->
            selectedUserIds.addAll(ids.toSet())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUsersSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.loadUsers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        binding.toolbar.title = "Выберите пользователей"
    }

    private fun setupRecyclerView() {
        binding.usersList.layoutManager = LinearLayoutManager(requireContext())
        binding.usersList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.users)
            binding.emptyState.isVisible = state.users.isEmpty() && !state.loading
            binding.progressBar.isVisible = state.loading && !state.refreshing
            if (state.error != null) {
                showError(state.error)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putLongArray(RESULT_SELECTED_IDS, selectedUserIds.toLongArray())
                }
            )
            dismiss()
        }
    }

    private fun showError(error: AppError) {
        val message = when (error) {
            is AppError.ApiError -> error.message ?: "Ошибка загрузки"
            is AppError.NetworkError -> "Нет соединения с сетью"
            else -> error.message ?: "Неизвестная ошибка"
        }
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        binding.emptyState.isVisible = true
        binding.emptyState.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "users_selection_request"
        const val RESULT_SELECTED_IDS = "selectedUserIds"
        const val ARG_SELECTED_IDS = "selectedUserIds"

        fun newInstance(selectedIds: LongArray = longArrayOf()): UsersSelectionDialogFragment {
            return UsersSelectionDialogFragment().apply {
                arguments = Bundle().apply {
                    putLongArray(ARG_SELECTED_IDS, selectedIds)
                }
            }
        }
    }
}