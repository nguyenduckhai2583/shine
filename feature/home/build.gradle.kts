plugins {
    alias(libs.plugins.shine.android.feature)
}

android {
    namespace = "com.example.shine.feature.home"
}

dependencies {
    implementation(libs.coil.compose)
}
