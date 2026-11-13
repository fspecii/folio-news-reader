package dev.folio.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.folio.news.data.Article
import dev.folio.news.ui.*

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = isDark(vm.themeMode)
            val view = LocalView.current
            SideEffect {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
            FolioTheme(dark) {
                // Surface supplies the default content colour: without it plain Text is black, which is invisible in dark mode
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                  Box(Modifier.fillMaxSize()) {
                    HomeScreen(vm)

                    // keep the last article around so the exit animation still has content
                    var shown by remember { mutableStateOf<Article?>(null) }
                    if (vm.openArticle != null) shown = vm.openArticle
                    AnimatedVisibility(
                        visible = vm.openArticle != null,
                        enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.94f),
                        exit = fadeOut(tween(180)) + scaleOut(tween(200), targetScale = 0.97f),
                    ) {
                        shown?.let { a ->
                            key(a.id) {
                                ReaderScreen(vm, a, vm.nextOf(a), { n -> vm.advanceTo(n) }) { vm.openArticle = null }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = vm.showSources,
                        enter = slideInHorizontally(tween(260)) { it } + fadeIn(),
                        exit = slideOutHorizontally(tween(220)) { it } + fadeOut(),
                    ) { SourcesScreen(vm) }

                    AnimatedVisibility(
                        visible = vm.showMagazines,
                        enter = slideInHorizontally(tween(260)) { it } + fadeIn(),
                        exit = slideOutHorizontally(tween(220)) { it } + fadeOut(),
                    ) { MagazinesScreen(vm) }

                    if (vm.showSections) SectionsDialog(vm)
                    vm.saveTarget?.let { SaveToMagazineDialog(vm, it) }
                    UpdateDialog(vm)
                    LaunchedEffect(vm.onboarded) { if (vm.onboarded) vm.checkForUpdate() }

                    if (vm.showSettings) SettingsSheet(vm)
                    if (vm.showWizard) { OnboardingScreen(vm); BackHandler(vm.onboarded) { vm.showWizard = false } }

                    BackHandler(vm.openArticle != null) { vm.openArticle = null }
                    BackHandler(vm.showSources && vm.openArticle == null) { vm.closeSources() }
                  }
                }
            }
        }
    }
}
