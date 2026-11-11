plugins {
    alias(libs.plugins.shine.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shine.hilt)
}

android {
    namespace = "com.example.shine.core.network"

    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"https://employer-api.dev.hodfords.uk/\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.core.model)
    api(projects.core.datastore)
    implementation(projects.core.common)
    implementation(projects.core.database)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json")
    }

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.datastore.preferences)
}
