plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shine.hilt)
}

android {
    namespace = "com.example.shine.core.data"

    defaultConfig {
        buildConfigField("String", "PASSWORD_SALT", "\"709d0a32e4d0b1a2ce8800998d95d6f9\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.okhttp.mockwebserver)
}
