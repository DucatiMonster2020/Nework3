package ru.netology.nework.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.adapter.UserProfilePagerAdapter
import ru.netology.nework.databinding.DialogAddJobBinding
import ru.netology.nework.databinding.FragmentUserProfileBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.error.AppError
import ru.netology.nework.viewmodel.MyProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MyProfileFragment : Fragment() {

    private val viewModel by viewModels<MyProfileViewModel>()
    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupViewPager()
        setupObservers()
        setupListeners()

        viewModel.loadMyProfile()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Мой профиль"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupViewPager() {
        val adapter = UserProfilePagerAdapter(this, userId = 0, isCurrentUser = true)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Стена"
                1 -> "Работы"
                else -> ""
            }
        }.attach()
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userName.text = it.name
                binding.userLogin.text = "@${it.login}"

                if (!it.avatar.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(it.avatar)
                        .circleCrop()
                        .placeholder(R.drawable.author_avatar)
                        .error(R.drawable.author_avatar)
                        .into(binding.userAvatar)
                } else {
                    binding.userAvatar.setImageResource(R.drawable.author_avatar)
                }
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.userAvatar.isVisible = !loading
            binding.userName.isVisible = !loading
            binding.userLogin.isVisible = !loading
            binding.tabLayout.isVisible = !loading
            binding.viewPager.isVisible = !loading
            binding.fabAddJob.isVisible = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }
    private fun showError(error: AppError) {
        val message = when (error) {
            is AppError.ApiError -> error.message ?: "Ошибка загрузки"
            is AppError.NetworkError -> "Нет соединения с сетью"
            else -> error.message ?: "Неизвестная ошибка"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setupListeners() {
        binding.fabAddJob.setOnClickListener {
            showAddJobDialog()
        }
    }

    private fun showAddJobDialog() {
        val dialogBinding = DialogAddJobBinding.inflate(layoutInflater)
        var startDate: Calendar? = null
        var endDate: Calendar? = null
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        dialogBinding.startDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    startDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    dialogBinding.startDateButton.text = dateFormat.format(startDate!!.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.endDateButton.setOnClickListener {
            if (dialogBinding.currentJobCheckBox.isChecked) {
                Toast.makeText(requireContext(), "Уберите отметку 'по настоящее время'", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    endDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }

                    if (startDate != null && endDate != null && endDate!! < startDate!!) {
                        Toast.makeText(requireContext(), "Дата окончания должна быть позже даты начала", Toast.LENGTH_SHORT).show()
                        endDate = null
                        return@DatePickerDialog
                    }

                    dialogBinding.endDateButton.text = dateFormat.format(endDate!!.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.currentJobCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                endDate = null
                dialogBinding.endDateButton.text = "По настоящее время"
                dialogBinding.endDateButton.isEnabled = false
            } else {
                dialogBinding.endDateButton.text = "Выберите дату окончания"
                dialogBinding.endDateButton.isEnabled = true
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить работу")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = dialogBinding.companyEditText.text.toString().trim()
                val position = dialogBinding.positionEditText.text.toString().trim()

                when {
                    name.isEmpty() -> {
                        Toast.makeText(requireContext(), "Введите название компании", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    position.isEmpty() -> {
                        Toast.makeText(requireContext(), "Введите должность", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    startDate == null -> {
                        Toast.makeText(requireContext(), "Выберите дату начала", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }

                val job = Job(
                    id = 0,
                    name = name,
                    position = position,
                    start = dateFormat.format(startDate!!.time),
                    finish = if (dialogBinding.currentJobCheckBox.isChecked) null
                    else endDate?.let { dateFormat.format(it.time) },
                    link = dialogBinding.linkEditText.text.toString().trim()
                        .takeIf { it.isNotEmpty() }
                )

                lifecycleScope.launch {
                    viewModel.saveJob(job)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}