package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.JobsAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUserJobsBinding
import ru.netology.nework.viewmodel.UserJobsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserJobsFragment : Fragment() {

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_IS_CURRENT_USER = "is_current_user"

        fun newInstance(userId: Long, isCurrentUser: Boolean = false): UserJobsFragment {
            return UserJobsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                    putBoolean(ARG_IS_CURRENT_USER, isCurrentUser)
                }
            }
        }
    }

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel by viewModels<UserJobsViewModel>()
    private var _binding: FragmentUserJobsBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy {
        JobsAdapter(
            onItemClickListener = { job ->
                // По ТЗ: просто показываем информацию в карточке
                Snackbar.make(binding.root, "${job.name}: ${job.position}", Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    private var userId: Long = 0
    private var isCurrentUser: Boolean = false

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

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        loadJobs()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.title = getString(R.string.user_jobs)
        }
        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().popBackStack()) {
                activity?.onBackPressed()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.jobsList.layoutManager = LinearLayoutManager(requireContext())
        binding.jobsList.adapter = adapter
        binding.jobsList.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        )
    }

    private fun setupObservers() {
        viewModel.jobs.observe(viewLifecycleOwner) { jobs ->
            adapter.submitList(jobs)
            binding.emptyState.isVisible = jobs.isEmpty()
            updateToolbarTitle(jobs.size)
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.swipeRefresh.isRefreshing = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { loadJobs() }
                    .show()
            }
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshJobs()
        }

        binding.addJobFab.apply {
            isVisible = isCurrentUser
            setOnClickListener {
                // Для своего профиля (п.7 ТЗ)
                Snackbar.make(binding.root, "Добавить работу", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.retryButton.setOnClickListener {
            loadJobs()
        }
    }

    private fun loadJobs() {
        lifecycleScope.launch {
            viewModel.loadJobs(userId)
        }
    }

    private fun refreshJobs() {
        lifecycleScope.launch {
            viewModel.refreshJobs(userId)
        }
    }

    private fun updateToolbarTitle(jobsCount: Int) {
        if (!isAdded) return
        val title = when (jobsCount) {
            0 -> getString(R.string.user_jobs)
            1 -> "1 работа"
            in 2..4 -> "$jobsCount работы"
            else -> "$jobsCount работ"
        }
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
    }

    override fun onResume() {
        super.onResume()
        refreshJobs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}