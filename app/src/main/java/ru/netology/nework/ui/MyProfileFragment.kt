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
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.UserProfilePagerAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUserDetailBinding
import ru.netology.nework.viewmodel.MyProfileViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MyProfileFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel by viewModels<MyProfileViewModel>()
    private var _binding: FragmentUserDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: UserProfilePagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTabs()
        setupObservers()
        loadProfile()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupTabs() {
        // Для своего профиля используем isCurrentUser = true
        val userId = appAuth.authState.value?.id ?: 0L
        pagerAdapter = UserProfilePagerAdapter(
            childFragmentManager,
            lifecycle,
            userId,
            isCurrentUser = true // Важно: свой профиль
        )

        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 2

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.my_wall)
                1 -> getString(R.string.my_jobs)
                else -> ""
            }
        }.attach()
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            updateUserInfo(user)
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.contentContainer.isVisible = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { loadProfile() }
                    .show()
            }
        }
    }

    private fun loadProfile() {
        val userId = appAuth.authState.value?.id ?: 0L
        if (userId != 0L) {
            lifecycleScope.launch {
                viewModel.loadUser(userId)
            }
        } else {
            findNavController().popBackStack()
        }
    }

    private fun updateUserInfo(user: ru.netology.nework.dto.User?) {
        user?.let {
            // Устанавливаем заголовок тулбара
            (activity as? AppCompatActivity)?.supportActionBar?.title = user.name
            binding.toolbar.subtitle = "@${user.login}"

            // Загружаем аватар
            if (!user.avatar.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(user.avatar)
                    .circleCrop()
                    .placeholder(R.drawable.author_avatar)
                    .error(R.drawable.author_avatar)
                    .into(binding.userAvatar)
            } else {
                binding.userAvatar.setImageResource(R.drawable.author_avatar)
            }

            // Обновляем информацию
            binding.userName.text = user.name
            binding.userLogin.text = "@${user.login}"
        }
    }

    // Для добавления работы (будет вызываться из UserJobsFragment)
    fun navigateToAddJob() {
        // TODO: Реализовать экран добавления работы
        Snackbar.make(binding.root, "Добавить работу", Snackbar.LENGTH_SHORT).show()
    }

    // Для редактирования работы
    fun navigateToEditJob(jobId: Long) {
        // TODO: Реализовать экран редактирования работы
        Snackbar.make(binding.root, "Редактировать работу $jobId", Snackbar.LENGTH_SHORT).show()
    }

    // Для удаления работы
    fun deleteJob(jobId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_job)
            .setMessage(R.string.delete_job_confirmation)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                lifecycleScope.launch {
                    viewModel.deleteJob(jobId)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        super.onDestroyView()
        _binding = null
    }
}