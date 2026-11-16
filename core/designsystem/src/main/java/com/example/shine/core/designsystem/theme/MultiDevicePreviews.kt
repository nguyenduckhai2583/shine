package com.example.shine.core.designsystem.theme

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotation representing multiple devices (Phone, Foldable, Tablet).
 */
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
annotation class MultiDevicePreviews
