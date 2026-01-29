package ru.netology.nework

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.ActivityMainBinding
import ru.netology.nework.viewmodel.AuthViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appAuth: AppAuth

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupNavigation()
        setupObservers()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Настройка BottomNavigationView (ТЗ: нижнее меню с тремя кнопками)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // Настройка кнопки "Назад" в Toolbar
        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar?.setDisplayHomeAsUpEnabled(
                destination.id != R.id.postsFragment &&
                        destination.id != R.id.eventsFragment &&
                        destination.id != R.id.usersFragment
            )
        }
    }

    private fun setupObservers() {
        // Наблюдаем за состоянием авторизации
        viewModel.authState.observe(this) { authState ->
            invalidateOptionsMenu() // Перерисовываем меню при изменении авторизации
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Формируем меню в зависимости от состояния аутентификации (ТЗ)
        val isAuthorized = appAuth.authState.value?.id != 0L

        menu.findItem(R.id.sign_in).isVisible = !isAuthorized
        menu.findItem(R.id.sign_up).isVisible = !isAuthorized
        menu.findItem(R.id.profile).isVisible = isAuthorized
        menu.findItem(R.id.sign_out).isVisible = isAuthorized

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.sign_in -> {
                navigateToSignIn()
                true
            }
            R.id.sign_up -> {
                navigateToSignUp()
                true
            }
            R.id.profile -> {
                navigateToProfile()
                true
            }
            R.id.sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun navigateToSignIn() {
        // Простая навигация к экрану входа
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Если мы не на экране входа - переходим
        val currentDestination = navController.currentDestination?.id
        if (currentDestination != R.id.signInFragment) {
            navController.navigate(R.id.signInFragment)
        }
    }

    private fun navigateToSignUp() {
        // Простая навигация к экрану регистрации
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val currentDestination = navController.currentDestination?.id
        if (currentDestination != R.id.signUpFragment) {
            navController.navigate(R.id.signUpFragment)
        }
    }

    private fun navigateToProfile() {
        val userId = appAuth.authState.value?.id ?: 0L
        if (userId != 0L) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController

            Snackbar.make(
                binding.root,
                "Переход к своему профилю",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun signOut() {
        // Выход из аккаунта
        appAuth.setAuth(null)
        Snackbar.make(
            binding.root,
            "Вы вышли из аккаунта",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}