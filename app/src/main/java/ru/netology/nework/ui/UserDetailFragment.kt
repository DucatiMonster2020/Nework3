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
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.UserProfilePagerAdapter
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentUserDetailBinding
import ru.netology.nework.dto.User
import ru.netology.nework.viewmodel.UserDetailViewModel
import javax.inject.Inject

@AndroidEntryPoint
class UserDetailFragment : Fragment() {

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_IS_CURRENT_USER = "is_current_user"

        fun newInstance(userId: Long, isCurrentUser: Boolean = false): UserDetailFragment {
            return UserDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                    putBoolean(ARG_IS_CURRENT_USER, isCurrentUser)
                }
            }
        }
    }

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel by viewModels<UserDetailViewModel>()
    private var _binding: FragmentUserDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var pagerAdapter: UserProfilePagerAdapter
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
        _binding = FragmentUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTabs()
        setupObservers()
        loadUserData()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            if (!findNavController().popBackStack()) {
                activity?.onBackPressed()
            }
        }
    }

    private fun setupTabs() {
        pagerAdapter = UserProfilePagerAdapter(
            childFragmentManager,
            lifecycle,
            userId,
            isCurrentUser
        )
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.wall_tab)
                1 -> getString(R.string.jobs_tab)
                else -> ""
            }
        }.attach()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
            }
        })
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            updateUserInfo(user)
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.userAvatar.isVisible = !loading
            binding.userName.isVisible = !loading
            binding.userLogin.isVisible = !loading
            binding.tabLayout.isVisible = !loading
            binding.viewPager.isVisible = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { loadUserData() }
                    .show()
            }
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            viewModel.loadUser(userId)
        }
    }

    private fun updateUserInfo(user: User?) {
        user?.let {
            (activity as? AppCompatActivity)?.supportActionBar?.title = user.name
            binding.toolbar.subtitle = "@${user.login}"
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
            binding.userName.text = user.name
            binding.userLogin.text = "@${user.login}"
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    override fun onDestroyView() {
        binding.viewPager.adapter = null
        super.onDestroyView()
        _binding = null
    }
}