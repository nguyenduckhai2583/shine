package com.example.shine.ui.profile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.shine.ui.common.ImagePickerBottomSheet
import com.example.shine.ui.theme.ShineTheme
import kotlin.math.sqrt

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onBack = onBack,
        onAvatarSelected = viewModel::onAvatarSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onAvatarSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showImagePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            val avatarRadius = 64.dp
            // Dịch chuyển theo góc 45 độ: x = y = radius * sqrt(0.5f)
            val edgeOffset = avatarRadius * sqrt(0.5f)

            // Avatar container with camera action badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(avatarRadius * 2 + 32.dp),
            ) {
                // Avatar image / initials
                Box(
                    modifier = Modifier
                        .size(avatarRadius * 2)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showImagePicker = true },
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarSource = uiState.selectedAvatarUri ?: uiState.avatarUrl
                    if (avatarSource != null) {
                        AsyncImage(
                            model = avatarSource,
                            contentDescription = "Profile Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        val initial = uiState.name.take(1).uppercase().ifEmpty { "U" }
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Camera action badge overlay - đặt tại tâm mép viền avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = edgeOffset, y = edgeOffset)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                        )
                        .clickable { showImagePicker = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Update avatar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button for updating avatar
            FilledTonalButton(
                onClick = { showImagePicker = true },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Change Avatar")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // User name display
            Text(
                text = uiState.name.ifEmpty { "User" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Reusable Image Picker BottomSheet (Camera + Gallery)
    ImagePickerBottomSheet(
        visible = showImagePicker,
        onDismissRequest = { showImagePicker = false },
        onImagePicked = onAvatarSelected,
        title = "Profile Photo",
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    ShineTheme(dynamicColor = false) {
        ProfileScreen(
            uiState = ProfileUiState(
                name = "Jane Doe",
                avatarUrl = null,
                selectedAvatarUri = null,
            ),
            onBack = {},
            onAvatarSelected = {},
        )
    }
}
