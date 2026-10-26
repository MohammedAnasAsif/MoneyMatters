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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mohammedanasasif.moneymatters.ui.home.HomeScreen
import io.github.mohammedanasasif.moneymatters.ui.home.HomeViewModel
import io.github.mohammedanasasif.moneymatters.ui.home.ReviewScreen
import io.github.mohammedanasasif.moneymatters.ui.home.TickerDetailScreen
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

private enum class Screen { HOME, DETAIL, SETTINGS, REVIEW }

@Composable
private fun AppRoot() {
    val vm: HomeViewModel = viewModel()
    // Plain remember: the detail screen's selection lives in the ViewModel and isn't restored.
    var screen by remember { mutableStateOf(Screen.HOME) }
    val goHome = {
        vm.closeDetail()
        screen = Screen.HOME
    }
    BackHandler(enabled = screen != Screen.HOME) { goHome() }

    Surface(color = AppColors.Background, modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.HOME -> HomeScreen(
                vm = vm,
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenReview = { screen = Screen.REVIEW },
                onOpenTicker = { id ->
                    vm.openDetail(id)
                    screen = Screen.DETAIL
                },
            )
            Screen.DETAIL -> TickerDetailScreen(vm = vm, onBack = goHome)
            Screen.SETTINGS -> SettingsScreen(onBack = goHome)
            Screen.REVIEW -> ReviewScreen(vm = vm, onBack = goHome)
        }
    }
}
