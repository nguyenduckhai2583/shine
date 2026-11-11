plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.android.library.compose)
}

android {
    namespace = "com.example.shine.core.designsystem"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
