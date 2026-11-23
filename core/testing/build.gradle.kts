plugins {
    alias(libs.plugins.shine.android.library)
}

android {
    namespace = "com.example.shine.core.testing"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    api(libs.androidx.paging.common)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
