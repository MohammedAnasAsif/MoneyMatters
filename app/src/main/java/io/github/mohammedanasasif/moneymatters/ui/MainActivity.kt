package io.github.mohammedanasasif.moneymatters.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mohammedanasasif.moneymatters.ui.home.HomeScreen
import io.github.mohammedanasasif.moneymatters.ui.home.HomeViewModel
import io.github.mohammedanasasif.moneymatters.ui.home.ReviewScreen
import io.github.mohammedanasasif.moneymatters.ui.settings.SettingsScreen
import io.github.mohammedanasasif.moneymatters.ui.theme.AppColors
import io.github.mohammedanasasif.moneymatters.ui.theme.MoneyMattersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MoneyMattersTheme { AppRoot() }
        }
    }
}

private enum class Screen { HOME, SETTINGS, REVIEW }

@Composable
private fun AppRoot() {
    val vm: HomeViewModel = viewModel()
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Surface(color = AppColors.Background, modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                vm = vm,
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenReview = { screen = Screen.REVIEW },
            )
            Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.HOME })
            Screen.REVIEW -> ReviewScreen(vm = vm, onBack = { screen = Screen.HOME })
        }
    }
}
