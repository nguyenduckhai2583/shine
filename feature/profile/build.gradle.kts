plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.android.library.compose)
    alias(libs.plugins.shine.hilt)
    alias(libs.plugins.shine.android.test)
}

android {
    namespace = "com.example.shine.feature.profile"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)

    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(projects.core.model)
}
