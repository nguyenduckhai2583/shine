import com.android.build.api.dsl.CommonExtension
import com.example.shine.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

@Suppress("unused")
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.findByType<CommonExtension>()?.apply {
                testOptions.apply {
                    unitTests.isReturnDefaultValues = true
                    unitTests.all {
                        it.systemProperty("net.bytebuddy.experimental", "true")
                    }
                }
            }

            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("testImplementation", libs.findLibrary("turbine").get())
                add("testImplementation", libs.findLibrary("mockito-core").get())
            }
        }
    }
}
