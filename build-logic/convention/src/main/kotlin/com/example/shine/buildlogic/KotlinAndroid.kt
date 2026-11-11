package com.example.shine.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureKotlinAndroid(
    libraryExtension: LibraryExtension,
) {
    libraryExtension.apply {
        compileSdk {
            version = release(37)
        }

        defaultConfig {
            minSdk = 24
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
        add("testImplementation", libs.findLibrary("junit").get())
        add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
    }
}
