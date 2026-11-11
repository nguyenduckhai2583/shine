plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.android.library.compose)
    alias(libs.plugins.shine.hilt)
}

android {
    namespace = "com.example.shine.feature.home"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.designsystem)

    implementation(libs.coil.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
}
