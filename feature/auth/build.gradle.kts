plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.android.library.compose)
    alias(libs.plugins.shine.hilt)
}

android {
    namespace = "com.example.shine.feature.auth"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)

    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(projects.core.model)
}
