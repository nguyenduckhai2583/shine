plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.shine.hilt)
}

android {
    namespace = "com.example.shine.core.database"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.paging.runtime)
}
