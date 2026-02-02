package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentSignInBinding
import ru.netology.nework.viewmodel.SignInViewModel
import javax.inject.Inject

@AndroidEntryPoint
class SignInFragment : Fragment() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel by viewModels<SignInViewModel>()
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
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
            binding.signInButton.isEnabled = !loading
            binding.loginInput.isEnabled = !loading
            binding.passwordInput.isEnabled = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (it.contains("400")) {
                    Snackbar.make(binding.root, "Неправильный логин или пароль", Snackbar.LENGTH_LONG).show()
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
        binding.signInButton.setOnClickListener {
            signIn()
        }

        binding.signUpButton.setOnClickListener {
            findNavController().navigate(R.id.signUpFragment)
        }
        binding.loginInput.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateLogin()
            }
        }

        binding.passwordInput.editText?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePassword()
            }
        }
    }

    private fun signIn() {
        val login = binding.loginInput.editText?.text.toString().trim()
        val password = binding.passwordInput.editText?.text.toString().trim()

        if (!validateLogin() || !validatePassword()) {
            return
        }

        lifecycleScope.launch {
            viewModel.signIn(login, password)
        }
    }

    private fun validateLogin(): Boolean {
        val login = binding.loginInput.editText?.text.toString().trim()
        return if (login.isEmpty()) {
            binding.loginInput.error = "Логин не может быть пустым"
            false
        } else {
            binding.loginInput.error = null
            true
        }
    }

    private fun validatePassword(): Boolean {
        val password = binding.passwordInput.editText?.text.toString().trim()
        return if (password.isEmpty()) {
            binding.passwordInput.error = "Пароль не может быть пустым"
            false
        } else {
            binding.passwordInput.error = null
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}