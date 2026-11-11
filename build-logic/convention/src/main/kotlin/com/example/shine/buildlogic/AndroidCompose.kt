package com.example.shine.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(
    libraryExtension: LibraryExtension,
) {
    libraryExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-compose-ui").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("androidx-compose-material3").get())
        add("implementation", libs.findLibrary("androidx-compose-material-icons-extended").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    }
}
