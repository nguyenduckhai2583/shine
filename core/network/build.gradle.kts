plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.shine.core.network"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        buildConfigField("String", "BASE_URL", "\"https://employer-api.dev.hodfords.uk/\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.systemProperty("net.bytebuddy.experimental", "true")
        }
    }
}

dependencies {
    api(project(":core:model"))
    api(project(":core:datastore"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json")
    }

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.datastore.preferences)
}
