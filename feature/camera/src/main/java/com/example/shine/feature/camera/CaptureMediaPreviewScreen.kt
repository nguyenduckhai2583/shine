package com.example.shine.feature.camera

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.core.net.toUri
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CaptureMediaPreviewRoute(
    media: CapturedMedia,
    onRetake: () -> Unit,
    onConfirm: (CapturedMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var videoThumbnail by remember(media) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(media) {
        if (media is CapturedMedia.Video) {
            withContext(Dispatchers.IO) {
                videoThumbnail = CameraMediaUtils.getVideoThumbnail(context, media.uri.toUri())
            }
        }
    }

    CaptureMediaPreviewScreen(
        media = media,
        videoThumbnail = videoThumbnail,
        onRetake = {
            runCatching {
                context.contentResolver.delete(media.uri.toUri(), null, null)
            }
            onRetake()
        },
        onConfirm = { onConfirm(media) },
        modifier = modifier,
    )
}

@Composable
fun CaptureMediaPreviewScreen(
    media: CapturedMedia,
    videoThumbnail: Bitmap?,
    onRetake: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVideoPlaying by rememberSaveable { mutableStateOf(value = true) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val isLandscape = maxWidth > maxHeight
        val mediaContent: @Composable () -> Unit = {
            MediaPreviewContent(
                media = media,
                videoThumbnail = videoThumbnail,
                isPlaying = isVideoPlaying,
                onTogglePlay = { isVideoPlaying = !isVideoPlaying },
            )
        }
        val playPauseButton: (@Composable () -> Unit)? = if (media is CapturedMedia.Video) {
            {
                PlayPauseButton(
                    isPlaying = isVideoPlaying,
                    onTogglePlay = { isVideoPlaying = !isVideoPlaying },
                )
            }
        } else null

        if (isLandscape) {
            LandscapeLayout(
                mediaContent = mediaContent,
                retakeButton = { RetakeButton(onRetake = onRetake) },
                confirmButton = { ConfirmButton(onConfirm = onConfirm) },
                playPauseButton = playPauseButton,
            )
        } else {
            PortraitLayout(
                mediaContent = mediaContent,
                retakeButton = { RetakeButton(onRetake = onRetake) },
                confirmButton = { ConfirmButton(onConfirm = onConfirm) },
                playPauseButton = playPauseButton,
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    mediaContent: @Composable () -> Unit,
    retakeButton: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    playPauseButton: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            mediaContent()
        }

        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
                .fillMaxHeight()
                .width(112.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
            ) {
                playPauseButton?.invoke()
                retakeButton()
                confirmButton()
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    mediaContent: @Composable () -> Unit,
    retakeButton: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    playPauseButton: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            mediaContent()
        }

        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxWidth(),
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
                    retakeButton()
                }

                playPauseButton?.invoke()

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    confirmButton()
                }
            }
        }
    }
}

@Composable
private fun MediaPreviewContent(
    media: CapturedMedia,
    videoThumbnail: Bitmap?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (media) {
        is CapturedMedia.Photo -> {
            AsyncImage(
                model = media.uri,
                contentDescription = "Captured Photo",
                contentScale = ContentScale.Fit,
                modifier = modifier.fillMaxSize(),
            )
        }

        is CapturedMedia.Video -> {
            VideoPlayerWidget(
                uri = media.uri.toUri(),
                thumbnail = videoThumbnail,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun RetakeButton(onRetake: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onRetake,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.camera_retake),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConfirmButton(onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onConfirm,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.camera_use),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onTogglePlay,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}
