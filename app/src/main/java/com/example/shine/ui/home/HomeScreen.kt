package com.example.shine.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shine.ui.theme.ShineTheme

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState = uiState, onSignOut = viewModel::signOut)
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Home", style = MaterialTheme.typography.headlineMedium)
            if (uiState.displayName.isNotEmpty()) {
                Text("Welcome, ${uiState.displayName}", style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ShineTheme(dynamicColor = false) {
        HomeScreen(uiState = HomeUiState(displayName = "Jane Doe"), onSignOut = {})
    }
}
