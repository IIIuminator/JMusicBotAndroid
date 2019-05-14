plugins {
    kotlin("jvm")
    kotlin("kapt")
    maven
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

dependencies {
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_core)

    api(Libs.timber_jdk)
    api(Libs.statemachine)

    implementation(Libs.dagger)
    kapt(Libs.dagger_compiler)

    implementation(Libs.java_jwt)
    implementation(Libs.okhttp)
    implementation(Libs.logging_interceptor)
    implementation(Libs.retrofit)
    implementation(Libs.retrofit2_kotlin_coroutines_adapter)
    implementation(Libs.converter_moshi)
    implementation(Libs.moshi)
    kapt(Libs.moshi_kotlin_codegen)

    testImplementation(Libs.junit_jupiter)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.strikt_core)
    testImplementation(Libs.mockk)
}
