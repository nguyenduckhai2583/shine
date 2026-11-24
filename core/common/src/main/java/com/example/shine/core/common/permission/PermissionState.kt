package com.example.shine.core.common.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

enum class AppPermission(val manifestPermissions: List<String>) {
    Camera(listOf(Manifest.permission.CAMERA)),
    RecordAudio(listOf(Manifest.permission.RECORD_AUDIO)),
    Video(listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)),
}

sealed interface PermissionStatus {
    data object Idle : PermissionStatus
    data object Granted : PermissionStatus
    data object Denied : PermissionStatus
    data object PermanentDenied : PermissionStatus
}

@Composable
fun rememberPermissionState(
    permission: AppPermission,
    onResult: (PermissionStatus) -> Unit = {},
): PermissionState {
    val activity = LocalContext.current.findActivity()
    val requested = rememberSaveable(permission) { mutableStateOf(false) }

    val state = remember(permission, activity) { PermissionState(permission, activity, requested) }
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        state.refresh()
        currentOnResult(state.status)
    }

    state.launch = {
        val perms = permission.manifestPermissions
        if (perms.isEmpty()) {
            state.refresh()
            currentOnResult(state.status)
        } else {
            launcher.launch(perms.toTypedArray())
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state.refresh()
    }

    return state
}

@Stable
class PermissionState internal constructor(
    private val permission: AppPermission,
    private val activity: Activity,
    private val requestedState: MutableState<Boolean>,
) {
    var status: PermissionStatus by mutableStateOf(resolve())
        private set

    internal var launch: () -> Unit = {}

    fun request() {
        requestedState.value = true
        launch()
    }

    fun openSettings() = activity.openAppSettings()

    internal fun refresh() {
        status = resolve()
    }

    private fun resolve(): PermissionStatus {
        if (isGranted()) return PermissionStatus.Granted

        val shouldShowRationale = permission.manifestPermissions.any { perm ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        }
        if (shouldShowRationale) return PermissionStatus.Denied
        if (requestedState.value) return PermissionStatus.PermanentDenied

        return PermissionStatus.Idle
    }

    private fun isGranted(): Boolean = permission.manifestPermissions.all { perm ->
        ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("Permissions must be requested within the context of an Activity")
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        if (this@openAppSettings !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    startActivity(intent)
}
