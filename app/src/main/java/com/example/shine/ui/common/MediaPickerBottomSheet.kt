package com.example.shine.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shine.ui.camera.CameraCaptureMode
import com.example.shine.ui.camera.CapturedMedia
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onMediaPicked: (CapturedMedia) -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Select Media",
    allowedModes: Set<CameraCaptureMode> = setOf(CameraCaptureMode.PHOTO, CameraCaptureMode.VIDEO),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val visualMediaType = when {
        allowedModes.contains(CameraCaptureMode.PHOTO) && allowedModes.contains(CameraCaptureMode.VIDEO) -> {
            ActivityResultContracts.PickVisualMedia.ImageAndVideo
        }
        allowedModes.contains(CameraCaptureMode.VIDEO) -> {
            ActivityResultContracts.PickVisualMedia.VideoOnly
        }
        else -> {
            ActivityResultContracts.PickVisualMedia.ImageOnly
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val media = if (mimeType.startsWith("video/")) {
                CapturedMedia.Video(uri)
            } else {
                CapturedMedia.Photo(uri)
            }
            onMediaPicked(media)
        }
    }

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                ListItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismissRequest()
                                onOpenCamera()
                            }
                        },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = "Open Camera",
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    supportingContent = {
                        val desc = when {
                            allowedModes.contains(CameraCaptureMode.PHOTO) && allowedModes.contains(CameraCaptureMode.VIDEO) ->
                                "Capture photos or record videos"
                            allowedModes.contains(CameraCaptureMode.VIDEO) ->
                                "Record a video with camera"
                            else ->
                                "Take a photo with camera"
                        }
                        Text(desc)
                    },
                )

                ListItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            galleryPickerLauncher.launch(PickVisualMediaRequest(visualMediaType))
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismissRequest()
                            }
                        },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    headlineContent = {
                        Text(
                            text = "Choose from Gallery",
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    supportingContent = {
                        Text("Select from your device storage")
                    },
                )
            }
        }
    }
}
