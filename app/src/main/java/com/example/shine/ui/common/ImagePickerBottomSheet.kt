package com.example.shine.ui.common

import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri

/**
 * Reusable BottomSheet Composable cho việc chọn ảnh từ Camera hoặc Gallery.
 * Có thể tái sử dụng ở bất kỳ màn hình nào cần upload ảnh/avatar.
 *
 * @param visible Trạng thái hiển thị bottom sheet
 * @param onDismissRequest Callback khi đóng bottom sheet
 * @param onImagePicked Callback trả về Uri của ảnh đã chụp hoặc chọn thành công
 * @param title Tiêu đề hiển thị ở đầu sheet (mặc định: "Select Photo")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Select Photo",
) {
    if (!visible) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Lưu Uri tạm thời khi chụp ảnh từ camera (an toàn khi activity bị recreate)
    var currentPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uriString ->
                onImagePicked(uriString.toUri())
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            onImagePicked(uri)
        }
    }

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
                            val uri = createTempPictureUri(context)
                            currentPhotoUri = uri.toString()
                            takePictureLauncher.launch(uri)
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
                    Text("Take a new photo with camera")
                },
            )

            ListItem(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismissRequest()
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
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
                    Text("Select a photo from device gallery")
                },
            )
        }
    }
}

/**
 * Tạo Uri tạm thời thông qua FileProvider để lưu ảnh chụp từ Camera
 */
private fun createTempPictureUri(context: Context): Uri {
    val imageDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File.createTempFile("capture_", ".jpg", imageDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}
