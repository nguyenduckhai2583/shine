plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.android.library.compose)
    alias(libs.plugins.shine.hilt)
    alias(libs.plugins.shine.android.test)
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

    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.coil.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(projects.core.testing)
}
