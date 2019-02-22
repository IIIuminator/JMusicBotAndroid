import kotlin.String

/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val core_ktx: String = "1.0.1" 

    const val lifecycle_extensions: String = "2.0.0" 

    const val com_android_tools_build_gradle: String = "3.5.0-alpha03" 

    const val lint_gradle: String = "26.5.0-alpha03" 

    const val jwtdecode: String = "1.2.0" 

    const val com_github_dcendents_android_maven_gradle_plugin: String = "2.1" 

    const val retrofit2_kotlin_coroutines_adapter: String = "0.9.2" 

    const val timber: String = "4.7.1" 

    const val com_louiscad_splitties: String = "3.0.0-alpha03" 

    const val com_squareup_moshi: String = "1.8.0" 

    const val okhttp: String = "3.13.1" 

    const val com_squareup_retrofit2: String = "2.5.0" 

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2" 

    const val sentry_android_gradle_plugin: String = "1.7.21" 

    const val org_jetbrains_kotlin: String = "1.3.21" 

    const val kotlinx_coroutines_android: String = "1.1.1" 

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.2.1"

        const val currentVersion: String = "5.2.1"

        const val nightlyVersion: String = "5.4-20190222000101+0000"

        const val releaseCandidate: String = ""
    }
}
