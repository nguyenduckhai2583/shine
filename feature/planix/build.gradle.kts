plugins {
    alias(libs.plugins.shine.android.feature)
}

android {
    namespace = "com.example.shine.feature.planix"
}

dependencies {
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.androidx.paging.testing)
}
