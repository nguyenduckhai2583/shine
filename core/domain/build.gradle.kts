plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.hilt)
    alias(libs.plugins.shine.android.test)
}

android {
    namespace = "com.example.shine.core.domain"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.model)
}
