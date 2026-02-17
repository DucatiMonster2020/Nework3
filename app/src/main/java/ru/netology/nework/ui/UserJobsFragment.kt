package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.JobsAdapter
import ru.netology.nework.databinding.FragmentUserJobsBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants.ARG_IS_CURRENT_USER
import ru.netology.nework.utils.Constants.ARG_USER_ID
import ru.netology.nework.viewmodel.UserJobsViewModel

@AndroidEntryPoint
class UserJobsFragment : Fragment() {

    private var _binding: FragmentUserJobsBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<UserJobsViewModel>()
    private var userId: Long = 0
    private var isCurrentUser: Boolean = false

    private val adapter by lazy {
        JobsAdapter(
            onItemClickListener = { job ->
                if (isCurrentUser && job.ownedByMe) {
                    showJobMenu(job)
                }
            }
        )
    }

    companion object {
        fun newInstance(userId: Long, isCurrentUser: Boolean = false): UserJobsFragment {
            return UserJobsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                    putBoolean(ARG_IS_CURRENT_USER, isCurrentUser)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getLong(ARG_USER_ID)
            isCurrentUser = it.getBoolean(ARG_IS_CURRENT_USER, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        loadJobs()
    }

    private fun setupRecyclerView() {
        binding.jobsList.layoutManager = LinearLayoutManager(requireContext())
        binding.jobsList.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.jobs.observe(viewLifecycleOwner) { jobs ->
            adapter.submitList(jobs)
            binding.emptyState.isVisible = jobs.isEmpty()
            binding.jobsList.isVisible = jobs.isNotEmpty()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun loadJobs() {
        viewModel.loadJobs(userId)
    }

    private fun showJobMenu(job: Job) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(job.name)
            .setItems(arrayOf(getString(R.string.delete))) { _, _ ->
                confirmDeleteJob(job.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteJob(jobId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_job)
            .setMessage(R.string.delete_job_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteJob(jobId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showError(error: AppError) {
        val message = when (error) {
            is AppError.ApiError -> error.message ?: "Ошибка загрузки"
            is AppError.NetworkError -> "Нет соединения с сетью"
            else -> error.message ?: "Неизвестная ошибка"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}