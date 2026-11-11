plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shine.hilt)
}

android {
    namespace = "com.example.shine.core.datastore"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
