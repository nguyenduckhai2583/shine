package com.example.shine.ui.camera

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun VideoPlayerWidget(
    uri: Uri,
    modifier: Modifier = Modifier,
    thumbnail: Bitmap? = null,
    isPlaying: Boolean = true,
    onTogglePlay: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPrepared by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(isPlaying, isPrepared) {
        if (!isPrepared) return@LaunchedEffect
        val view = videoViewRef ?: return@LaunchedEffect
        if (isPlaying) {
            if (!view.isPlaying) view.start()
        } else {
            if (view.isPlaying) view.pause()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    videoViewRef?.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying && isPrepared) {
                        videoViewRef?.start()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoViewRef?.stopPlayback()
            videoViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onTogglePlay()
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.Gravity.CENTER,
                    )
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        isPrepared = true
                        if (isPlaying) {
                            start()
                        }
                    }
                    setOnCompletionListener {
                        if (isPlaying) {
                            start()
                        }
                    }
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!isPrepared && thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = "Video Thumbnail",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = isPrepared && !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(64.dp),
            )
        }
    }
}
