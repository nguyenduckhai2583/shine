plugins {
    alias(libs.plugins.shine.android.feature)
}

android {
    namespace = "com.example.shine.feature.profile"
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation3.runtime)

    testImplementation(libs.mockito.core)
}
