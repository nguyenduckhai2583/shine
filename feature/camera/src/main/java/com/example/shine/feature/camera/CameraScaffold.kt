package com.example.shine.feature.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CameraScaffold(
    isLandscape: Boolean,
    viewfinder: @Composable () -> Unit,
    recordingIndicator: @Composable () -> Unit,
    closeButton: @Composable () -> Unit,
    modeSwitch: @Composable () -> Unit,
    flipButton: @Composable () -> Unit,
    shutterButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isLandscape) {
        LandscapeLayout(
            viewfinder = viewfinder,
            recordingIndicator = recordingIndicator,
            closeButton = closeButton,
            modeSwitch = modeSwitch,
            flipButton = flipButton,
            shutterButton = shutterButton,
            modifier = modifier,
        )
    } else {
        PortraitLayout(
            viewfinder = viewfinder,
            recordingIndicator = recordingIndicator,
            closeButton = closeButton,
            modeSwitch = modeSwitch,
            flipButton = flipButton,
            shutterButton = shutterButton,
            modifier = modifier,
        )
    }
}

@Composable
private fun LandscapeLayout(
    viewfinder: @Composable () -> Unit,
    recordingIndicator: @Composable () -> Unit,
    closeButton: @Composable () -> Unit,
    modeSwitch: @Composable () -> Unit,
    flipButton: @Composable () -> Unit,
    shutterButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            viewfinder()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            ) {
                recordingIndicator()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.Bottom),
        ) {
            closeButton()
            modeSwitch()
            shutterButton()
            flipButton()
        }
    }
}

@Composable
private fun PortraitLayout(
    viewfinder: @Composable () -> Unit,
    recordingIndicator: @Composable () -> Unit,
    closeButton: @Composable () -> Unit,
    modeSwitch: @Composable () -> Unit,
    flipButton: @Composable () -> Unit,
    shutterButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            viewfinder()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
            ) {
                recordingIndicator()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                shutterButton()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            closeButton()
            modeSwitch()
            flipButton()
        }
    }
}
