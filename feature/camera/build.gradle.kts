plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.android.library.compose)
    alias(libs.plugins.shine.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shine.android.test)
}

android {
    namespace = "com.example.shine.feature.camera"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)
}
