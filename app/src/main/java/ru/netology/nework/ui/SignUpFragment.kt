package ru.netology.nework.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentSignUpBinding
import ru.netology.nework.viewmodel.SignUpViewModel

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    private val viewModel by viewModels<SignUpViewModel>()
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            loadImage(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            binding.signUpButton.isEnabled = !loading
            binding.loginInput.isEnabled = !loading
            binding.nameInput.isEnabled = !loading
            binding.passwordInput.isEnabled = !loading
            binding.passwordConfirmInput.isEnabled = !loading
            binding.avatarButton.isEnabled = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (it.contains("400")) {
                    Snackbar.make(binding.root, "Пользователь с таким логином уже зарегистрирован", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupListeners() {
        binding.signUpButton.setOnClickListener {
            signUp()
        }

        binding.avatarButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        listOf(
            binding.loginInput,
            binding.nameInput,
            binding.passwordInput,
            binding.passwordConfirmInput
        ).forEach { inputLayout ->
            inputLayout.editText?.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    validateField(inputLayout)
                }
            }
        }
    }

    private fun signUp() {
        val login = binding.loginInput.editText?.text.toString().trim()
        val name = binding.nameInput.editText?.text.toString().trim()
        val password = binding.passwordInput.editText?.text.toString().trim()
        val passwordConfirm = binding.passwordConfirmInput.editText?.text.toString().trim()

        if (!validateAllFields()) {
            return
        }

        if (password != passwordConfirm) {
            binding.passwordConfirmInput.error = "Пароли не совпадают"
            return
        }

        lifecycleScope.launch {
            viewModel.signUp(
                login = login,
                name = name,
                password = password,
                avatarUri = selectedImageUri
            )
        }
    }

    private fun validateAllFields(): Boolean {
        return validateField(binding.loginInput) &&
                validateField(binding.nameInput) &&
                validateField(binding.passwordInput) &&
                validateField(binding.passwordConfirmInput)
    }

    private fun validateField(inputLayout: com.google.android.material.textfield.TextInputLayout): Boolean {
        val text = inputLayout.editText?.text.toString().trim()

        return when (inputLayout.id) {
            R.id.loginInput -> {
                if (text.isEmpty()) {
                    inputLayout.error = "Логин не может быть пустым"
                    false
                } else {
                    inputLayout.error = null
                    true
                }
            }
            R.id.nameInput -> {
                if (text.isEmpty()) {
                    inputLayout.error = "Имя не может быть пустым"
                    false
                } else {
                    inputLayout.error = null
                    true
                }
            }
            R.id.passwordInput -> {
                if (text.isEmpty()) {
                    inputLayout.error = "Пароль не может быть пустым"
                    false
                } else {
                    inputLayout.error = null
                    true
                }
            }
            R.id.passwordConfirmInput -> {
                if (text.isEmpty()) {
                    inputLayout.error = "Повторите пароль"
                    false
                } else {
                    inputLayout.error = null
                    true
                }
            }
            else -> true
        }
    }

    private fun loadImage(uri: Uri) {
        try {
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(R.drawable.author_avatar)
                .into(binding.avatarImage)

            binding.avatarImage.isVisible = true

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}