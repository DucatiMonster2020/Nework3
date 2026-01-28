package ru.netology.nework

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.auth.AuthStateManager
import ru.netology.nework.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Настройка навигации
        setupNavigation()
    }
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Настройка BottomNavigationView
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.sign_in -> {
                // TODO: переход к экрану входа
                navigateToSignIn()
                true
            }
            R.id.sign_up -> {
                // TODO: переход к экрану регистрации
                navigateToSignUp()
                true
            }
            R.id.profile -> {
                // TODO: переход к профилю
                navigateToProfile()
                true
            }
            R.id.sign_out -> {
                // TODO: выход из аккаунта
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun updateMenu() {
        val isAuthorized = AuthStateManager.authState.value is AuthStateManager.AuthState.Authorized

        invalidateOptionsMenu() // Перерисовываем меню
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isAuthorized = AuthStateManager.authState.value is AuthStateManager.AuthState.Authorized

        // Показываем/скрываем группы меню в зависимости от авторизации
        menu.findItem(R.id.sign_in)?.isVisible = !isAuthorized
        menu.findItem(R.id.sign_up)?.isVisible = !isAuthorized
        menu.findItem(R.id.profile)?.isVisible = isAuthorized
        menu.findItem(R.id.sign_out)?.isVisible = isAuthorized

        return super.onPrepareOptionsMenu(menu)
    }

    private fun navigateToSignIn() {
        // TODO: навигация к экрану входа
    }

    private fun navigateToSignUp() {
        // TODO: навигация к экрану регистрации
    }

    private fun navigateToProfile() {
        // TODO: навигация к профилю
    }

    private fun signOut() {
        // TODO: выход из аккаунта
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}