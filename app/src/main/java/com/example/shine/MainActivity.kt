package com.example.shine

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.shine.ui.navigation.AppViewModel
import com.example.shine.ui.navigation.ShineApp
import com.example.shine.ui.theme.ShineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appViewModel.handleDeepLink(intent?.data)
        setContent {
            ShineTheme {
                ShineApp(viewModel = appViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appViewModel.handleDeepLink(intent.data)
    }
}
