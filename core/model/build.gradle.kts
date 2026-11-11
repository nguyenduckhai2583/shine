plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.shine.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
