plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.android.library.compose)
    alias(libs.plugins.shine.hilt)
}

android {
    namespace = "com.example.shine.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
