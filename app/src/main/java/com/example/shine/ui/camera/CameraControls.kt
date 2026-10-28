package com.example.shine.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraRecordingIndicator(
    durationSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.65f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935).copy(alpha = alpha)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = CameraMediaUtils.formatDuration(durationSeconds),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SegmentedModeSwitch(
    captureMode: CameraCaptureMode,
    onModeSelected: (CameraCaptureMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val itemWidth = 80.dp
    val itemHeight = 36.dp
    val indicatorOffset by animateDpAsState(
        targetValue = if (captureMode == CameraCaptureMode.PHOTO) 0.dp else itemWidth,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 380f,
        ),
        label = "indicatorOffset",
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF262626),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.padding(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                    .size(width = itemWidth, height = itemHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF4A4A4A)),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SegmentItem(
                    title = "Camera",
                    isSelected = captureMode == CameraCaptureMode.PHOTO,
                    onClick = { onModeSelected(CameraCaptureMode.PHOTO) },
                    enabled = enabled,
                    modifier = Modifier.size(width = itemWidth, height = itemHeight),
                )

                SegmentItem(
                    title = "Video",
                    isSelected = captureMode == CameraCaptureMode.VIDEO,
                    onClick = { onModeSelected(CameraCaptureMode.VIDEO) },
                    enabled = enabled,
                    modifier = Modifier.size(width = itemWidth, height = itemHeight),
                )
            }
        }
    }
}

@Composable
private fun SegmentItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "segmentContent",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
fun CameraBottomBar(
    captureMode: CameraCaptureMode,
    allowedModes: Set<CameraCaptureMode>,
    isRecording: Boolean,
    onClose: () -> Unit,
    onModeSelected: (CameraCaptureMode) -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            enabled = !isRecording,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color.White,
            )
        }

        if (allowedModes.size > 1) {
            SegmentedModeSwitch(
                captureMode = captureMode,
                onModeSelected = onModeSelected,
                enabled = !isRecording,
            )
        }

        IconButton(
            onClick = onFlipCamera,
            enabled = !isRecording,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Cameraswitch,
                contentDescription = "Switch Camera",
                tint = Color.White,
            )
        }
    }
}

@Composable
fun CameraShutterButton(
    captureMode: CameraCaptureMode,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }

    val innerSize by animateDpAsState(
        targetValue = if (isRecording) 32.dp else 60.dp,
        label = "innerSize",
    )

    val innerCornerRadius by animateDpAsState(
        targetValue = if (isRecording) 8.dp else 30.dp,
        label = "innerCornerRadius",
    )

    val innerColor by animateColorAsState(
        targetValue = when {
            captureMode == CameraCaptureMode.VIDEO -> Color(0xFFE53935)
            else -> Color.White
        },
        label = "innerColor",
    )

    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale.value)
            .border(
                width = 4.dp,
                color = Color.White,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(RoundedCornerShape(innerCornerRadius))
                .background(innerColor),
        )
    }
}
