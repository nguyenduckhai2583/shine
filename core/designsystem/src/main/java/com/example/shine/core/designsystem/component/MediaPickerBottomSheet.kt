package com.example.shine.core.designsystem.component

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shine.core.designsystem.R
import kotlinx.coroutines.launch

enum class MediaPickerType {
    IMAGE_ONLY,
    VIDEO_ONLY,
    IMAGE_AND_VIDEO,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onMediaPicked: (Uri) -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
    mediaType: MediaPickerType = MediaPickerType.IMAGE_ONLY,
    title: String = defaultMediaPickerTitle(mediaType),
    skipPartiallyExpanded: Boolean = true,
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    val visualMediaType = when (mediaType) {
        MediaPickerType.IMAGE_ONLY -> ActivityResultContracts.PickVisualMedia.ImageOnly
        MediaPickerType.VIDEO_ONLY -> ActivityResultContracts.PickVisualMedia.VideoOnly
        MediaPickerType.IMAGE_AND_VIDEO -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            onMediaPicked(uri)
        }
    }

    fun dismissAndRun(action: () -> Unit) {
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                if (!sheetState.isVisible) {
                    onDismissRequest()
                }
            }
            action()
        }
    }

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            modifier = modifier,
        ) {
            MediaPickerSheetContent(
                title = title,
                mediaType = mediaType,
                onCameraClick = { dismissAndRun(onOpenCamera) },
                onGalleryClick = {
                    dismissAndRun {
                        mediaPickerLauncher.launch(PickVisualMediaRequest(visualMediaType))
                    }
                },
            )
        }
    }
}

@Composable
private fun defaultMediaPickerTitle(mediaType: MediaPickerType): String = when (mediaType) {
    MediaPickerType.IMAGE_ONLY -> stringResource(R.string.media_picker_title_photo)
    MediaPickerType.VIDEO_ONLY -> stringResource(R.string.media_picker_title_video)
    MediaPickerType.IMAGE_AND_VIDEO -> stringResource(R.string.media_picker_title_all)
}

@Composable
private fun MediaPickerSheetContent(
    title: String,
    mediaType: MediaPickerType,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    val cameraDescription = when (mediaType) {
        MediaPickerType.IMAGE_ONLY -> stringResource(R.string.media_picker_camera_desc_photo)
        MediaPickerType.VIDEO_ONLY -> stringResource(R.string.media_picker_camera_desc_video)
        MediaPickerType.IMAGE_AND_VIDEO -> stringResource(R.string.media_picker_camera_desc_all)
    }
    val galleryDescription = when (mediaType) {
        MediaPickerType.IMAGE_ONLY -> stringResource(R.string.media_picker_gallery_desc_photo)
        MediaPickerType.VIDEO_ONLY -> stringResource(R.string.media_picker_gallery_desc_video)
        MediaPickerType.IMAGE_AND_VIDEO -> stringResource(R.string.media_picker_gallery_desc_all)
    }

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
                .clickable(onClick = onCameraClick),
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(R.string.media_picker_open_camera),
                    fontWeight = FontWeight.Medium,
                )
            },
            supportingContent = {
                Text(cameraDescription)
            },
        )

        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onGalleryClick),
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(R.string.media_picker_choose_gallery),
                    fontWeight = FontWeight.Medium,
                )
            },
            supportingContent = {
                Text(galleryDescription)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaPickerSheetContentPreview() {
    MaterialTheme {
        Surface {
            MediaPickerSheetContent(
                title = "Select Photo",
                mediaType = MediaPickerType.IMAGE_ONLY,
                onCameraClick = {},
                onGalleryClick = {},
            )
        }
    }
}
