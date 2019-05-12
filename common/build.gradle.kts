plugins {
    kotlin("jvm")
    kotlin("kapt")
    maven
}

dependencies {
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_core)

    api(Libs.timber)
    api(Libs.statemachine)

    kapt(Libs.dagger_compiler)
    implementation(Libs.dagger)

    implementation(Libs.jwtdecode)
    implementation(Libs.okhttp)
    implementation(Libs.logging_interceptor)
    implementation(Libs.retrofit)
    implementation(Libs.retrofit2_kotlin_coroutines_adapter)
    implementation(Libs.converter_moshi)
    implementation(Libs.moshi)
    kapt(Libs.moshi_kotlin_codegen)

    testImplementation(Libs.assertj_core)
    testImplementation(Libs.mockk)
}
