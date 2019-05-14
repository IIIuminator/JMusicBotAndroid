import kotlin.String

/**
 * Generated by https://github.com/jmfayard/buildSrcVersions
 *
 * Update this file with
 *   `$ ./gradlew buildSrcVersions` */
object Libs {
    /**
     * http://developer.android.com/tools/extras/support-library.html */
    const val core_ktx: String = "androidx.core:core-ktx:" + Versions.core_ktx

    /**
     * https://developer.android.com/topic/libraries/architecture/index.html */
    const val lifecycle_extensions: String = "androidx.lifecycle:lifecycle-extensions:" +
            Versions.lifecycle_extensions

    /**
     * https://developer.android.com/studio */
    const val aapt2: String = "com.android.tools.build:aapt2:" + Versions.aapt2

    /**
     * https://developer.android.com/studio */
    const val com_android_tools_build_gradle: String = "com.android.tools.build:gradle:" +
            Versions.com_android_tools_build_gradle

    /**
     * https://developer.android.com/studio */
    const val lint_gradle: String = "com.android.tools.lint:lint-gradle:" + Versions.lint_gradle

    /**
     * https://github.com/auth0/java-jwt */
    const val java_jwt: String = "com.auth0:java-jwt:" + Versions.java_jwt

    const val com_diffplug_gradle_spotless_gradle_plugin: String =
            "com.diffplug.gradle.spotless:com.diffplug.gradle.spotless.gradle.plugin:" +
            Versions.com_diffplug_gradle_spotless_gradle_plugin

    const val com_github_dcendents_android_maven_gradle_plugin: String =
            "com.github.dcendents.android-maven:com.github.dcendents.android-maven.gradle.plugin:" +
            Versions.com_github_dcendents_android_maven_gradle_plugin

    /**
     * https://github.com/Tinder/StateMachine */
    const val statemachine: String = "com.github.tinder:statemachine:" + Versions.statemachine

    /**
     * https://github.com/google/dagger */
    const val dagger_compiler: String = "com.google.dagger:dagger-compiler:" +
            Versions.com_google_dagger

    /**
     * https://github.com/google/dagger */
    const val dagger: String = "com.google.dagger:dagger:" + Versions.com_google_dagger

    /**
     * https://github.com/JakeWharton/retrofit2-kotlin-coroutines-adapter/ */
    const val retrofit2_kotlin_coroutines_adapter: String =
            "com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:" +
            Versions.retrofit2_kotlin_coroutines_adapter

    const val timber_jdk: String = "com.jakewharton.timber:timber-jdk:" + Versions.timber_jdk

    const val splitties_fun_pack_android_base: String =
            "com.louiscad.splitties:splitties-fun-pack-android-base:" +
            Versions.splitties_fun_pack_android_base

    /**
     * https://github.com/square/moshi */
    const val moshi_kotlin_codegen: String = "com.squareup.moshi:moshi-kotlin-codegen:" +
            Versions.com_squareup_moshi

    /**
     * https://github.com/square/moshi */
    const val moshi: String = "com.squareup.moshi:moshi:" + Versions.com_squareup_moshi

    /**
     * https://github.com/square/okhttp */
    const val logging_interceptor: String = "com.squareup.okhttp3:logging-interceptor:" +
            Versions.com_squareup_okhttp3

    /**
     * https://github.com/square/okhttp */
    const val okhttp: String = "com.squareup.okhttp3:okhttp:" + Versions.com_squareup_okhttp3

    /**
     * https://github.com/square/retrofit/ */
    const val converter_moshi: String = "com.squareup.retrofit2:converter-moshi:" +
            Versions.com_squareup_retrofit2

    /**
     * https://github.com/square/retrofit/ */
    const val retrofit: String = "com.squareup.retrofit2:retrofit:" +
            Versions.com_squareup_retrofit2

    const val de_fayard_buildsrcversions_gradle_plugin: String =
            "de.fayard.buildSrcVersions:de.fayard.buildSrcVersions.gradle.plugin:" +
            Versions.de_fayard_buildsrcversions_gradle_plugin

    /**
     * http://mockk.io */
    const val mockk: String = "io.mockk:mockk:" + Versions.mockk

    /**
     * http://assertj.org */
    const val assertj_core: String = "org.assertj:assertj-core:" + Versions.assertj_core

    /**
     * https://kotlinlang.org/ */
    const val kotlin_android_extensions_runtime: String =
            "org.jetbrains.kotlin:kotlin-android-extensions-runtime:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_android_extensions: String =
            "org.jetbrains.kotlin:kotlin-android-extensions:" + Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_annotation_processing_gradle: String =
            "org.jetbrains.kotlin:kotlin-annotation-processing-gradle:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_gradle_plugin: String = "org.jetbrains.kotlin:kotlin-gradle-plugin:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_scripting_compiler_embeddable: String =
            "org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://kotlinlang.org/ */
    const val kotlin_stdlib_jdk8: String = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:" +
            Versions.org_jetbrains_kotlin

    /**
     * https://github.com/Kotlin/kotlinx.coroutines */
    const val kotlinx_coroutines_android: String =
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:" + Versions.org_jetbrains_kotlinx

    /**
     * https://github.com/Kotlin/kotlinx.coroutines */
    const val kotlinx_coroutines_core: String = "org.jetbrains.kotlinx:kotlinx-coroutines-core:" +
            Versions.org_jetbrains_kotlinx

    /**
     * https://github.com/Kotlin/kotlinx.coroutines */
    const val kotlinx_coroutines_test: String = "org.jetbrains.kotlinx:kotlinx-coroutines-test:" +
            Versions.org_jetbrains_kotlinx

    /**
     * https://junit.org/junit5/ */
    const val junit_jupiter: String = "org.junit.jupiter:junit-jupiter:" + Versions.junit_jupiter
}
