/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val core_ktx: String = "1.1.0-alpha05"

    const val lifecycle_extensions: String = "2.1.0-alpha04"

    const val aapt2: String = "3.5.0-alpha13-5435860"

    const val com_android_tools_build_gradle: String = "3.5.0-alpha13"

    const val lint_gradle: String = "26.5.0-alpha13"

    const val java_jwt: String = "3.8.0"

    const val com_diffplug_gradle_spotless_gradle_plugin: String = "3.23.0"

    const val com_github_dcendents_android_maven_gradle_plugin: String = "2.1"

    const val statemachine: String = "0.1.2"

    const val com_google_dagger: String = "2.22.1"

    const val retrofit2_kotlin_coroutines_adapter: String = "0.9.2"

    const val timber_jdk: String = "5.0.0-SNAPSHOT"

    const val splitties_fun_pack_android_base: String = "3.0.0-alpha06"

    const val com_squareup_moshi: String = "1.8.0"

    const val com_squareup_okhttp3: String = "3.14.1"

    const val com_squareup_retrofit2: String = "2.5.0"

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2"

    const val mockk: String = "1.9.3"

    const val strikt_core: String = "0.20.1"

    const val org_jetbrains_kotlin: String = "1.3.31"

    const val org_jetbrains_kotlinx: String = "1.2.1"

    const val junit_jupiter: String = "5.4.2"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.4.1"

        const val currentVersion: String = "5.4.1"

        const val nightlyVersion: String = "5.5-20190514000037+0000"

        const val releaseCandidate: String = ""
    }
}
