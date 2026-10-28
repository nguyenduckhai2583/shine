package com.example.shine.ui.camera

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.shine.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaPreviewRoute(
    media: CapturedMedia,
    onRetake: () -> Unit,
    onConfirm: (CapturedMedia) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var videoThumbnail by remember(media) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(media) {
        if (media is CapturedMedia.Video) {
            withContext(Dispatchers.IO) {
                videoThumbnail = CameraMediaUtils.getVideoThumbnail(context, media.uri)
            }
        }
    }

    MediaPreviewScreen(
        media = media,
        videoThumbnail = videoThumbnail,
        onRetake = {
            runCatching {
                context.contentResolver.delete(media.uri, null, null)
            }
            onRetake()
        },
        onConfirm = { onConfirm(media) },
        onClose = onClose ?: onRetake,
        modifier = modifier,
    )
}

@Composable
fun MediaPreviewScreen(
    media: CapturedMedia,
    videoThumbnail: Bitmap?,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    var isVideoPlaying by rememberSaveable { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (media) {
            is CapturedMedia.Photo -> {
                AsyncImage(
                    model = media.uri,
                    contentDescription = "Captured Photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is CapturedMedia.Video -> {
                VideoPlayerWidget(
                    uri = media.uri,
                    thumbnail = videoThumbnail,
                    isPlaying = isVideoPlaying,
                    onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    TextButton(
                        onClick = onRetake,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) {
                        Text(
                            text = stringResource(R.string.camera_retake),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (media is CapturedMedia.Video) {
                    IconButton(
                        onClick = { isVideoPlaying = !isVideoPlaying },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (isVideoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isVideoPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    TextButton(
                        onClick = onConfirm,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    ) {
                        Text(
                            text = stringResource(R.string.camera_use),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
