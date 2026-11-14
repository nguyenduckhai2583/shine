import com.android.build.api.dsl.ProductFlavor
import java.util.Properties

fun loadEnv(envName: String): Properties = Properties().apply {
    file("env/$envName.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun ProductFlavor.buildTypedConfigField(key: String, rawValue: String) {
    val (type, value) = when {
        rawValue.equals("true", ignoreCase = true) || rawValue.equals("false", ignoreCase = true) ->
            "boolean" to rawValue.lowercase()
        rawValue.toIntOrNull() != null ->
            "int" to rawValue
        rawValue.toLongOrNull() != null ->
            "long" to "${rawValue}L"
        else ->
            "String" to "\"$rawValue\""
    }
    buildConfigField(type, key, value)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.shine"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.shine"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("environment")

    productFlavors {
        val manifestKeys = setOf(
            "APP_NAME",
            "DEEP_LINK_HOST",
        )

        listOf("dev", "prod").forEach { envName ->
            create(envName) {
                dimension = "environment"
                val env = loadEnv(envName)

                env.getProperty("APP_SUFFIX")?.takeIf { it.isNotBlank() }?.let { suffix ->
                    applicationIdSuffix = suffix
                    versionNameSuffix = suffix
                }

                for (key in manifestKeys) {
                    env.getProperty(key)?.let { value ->
                        manifestPlaceholders[key] = value
                    }
                }

                for ((key, value) in env) {
                    buildTypedConfigField(key.toString(), value.toString())
                }
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.systemProperty("net.bytebuddy.experimental", "true")
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.feature.auth)
    implementation(projects.feature.planix)
    implementation(projects.feature.workspace)
    implementation(projects.feature.profile)
    implementation(projects.feature.channel)
    implementation(projects.feature.camera)
    implementation(projects.feature.home)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
