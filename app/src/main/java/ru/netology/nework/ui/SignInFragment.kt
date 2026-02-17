package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentSignInBinding
import ru.netology.nework.error.AppError
import ru.netology.nework.utils.Constants
import ru.netology.nework.viewmodel.SignInViewModel

@AndroidEntryPoint
class SignInFragment : Fragment() {

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

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.signInButton.setOnClickListener {
            val login = binding.loginInput.editText?.text.toString().trim()
            val password = binding.passwordInput.editText?.text.toString().trim()

            if (validateInputs(login, password)) {
                viewModel.signIn(login, password)
            }
        }

        binding.signUpButton.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.signInButton.isEnabled = !loading
            binding.signUpButton.isEnabled = !loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            when (error) {
                is AppError.ApiError -> {
                    if (error.code == 400) {
                        Toast.makeText(requireContext(), Constants.ERROR_INVALID_LOGIN_PASSWORD, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), error.message ?: "Ошибка", Toast.LENGTH_LONG).show()
                    }
                }
                is AppError.NetworkError -> {
                    Toast.makeText(requireContext(), "Ошибка сети", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(requireContext(), error.message ?: "Неизвестная ошибка", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigate(R.id.action_signInFragment_to_postsFragment)
            }
        }
    }

    private fun validateInputs(login: String, password: String): Boolean {
        var isValid = true

        if (login.isEmpty()) {
            binding.loginInput.error = getString(R.string.cannot_be_empty)
            isValid = false
        } else {
            binding.loginInput.error = null
        }

        if (password.isEmpty()) {
            binding.passwordInput.error = getString(R.string.cannot_be_empty)
            isValid = false
        } else {
            binding.passwordInput.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}