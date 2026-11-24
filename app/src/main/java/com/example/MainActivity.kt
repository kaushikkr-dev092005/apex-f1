package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.F1MainApp
import com.example.ui.F1ViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val appModel: F1ViewModel by viewModels()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel = appModel
      val appearance by viewModel.appearance.collectAsStateWithLifecycle()
      val isDarkTheme = when (appearance) { "light" -> false; "system" -> isSystemInDarkTheme(); else -> true }
      val density = LocalDensity.current
      SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
          isAppearanceLightStatusBars = !isDarkTheme
          isAppearanceLightNavigationBars = !isDarkTheme
        }
      }

      CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * if (viewModel.largeText) 1.13f else 1f)) {
      MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          F1MainApp(viewModel = viewModel)
        }
      }
      }
    }
    intent?.dataString?.let(appModel::route)
  }

  override fun onStart() { super.onStart(); appModel.setForeground(true) }
  override fun onStop() { appModel.setForeground(false); super.onStop() }
  override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); intent.dataString?.let(appModel::route) }
}
