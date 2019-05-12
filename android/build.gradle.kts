plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.github.dcendents.android-maven")
}

group = "com.ivoberger.jmusicbot.client"

android {
    compileSdkVersion(28)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            consumerProguardFile("consumer-proguard-rules.pro")
        }
    }
    packagingOptions.pickFirst("META-INF/atomicfu.kotlin_module")
}

dependencies {
    implementation(project(":common"))
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_android)
    implementation(Libs.core_ktx)
    implementation(Libs.lifecycle_extensions)

    kapt(Libs.dagger_compiler)
    implementation(Libs.dagger)

    implementation(Libs.splitties_fun_pack_android_base)
    testImplementation(Libs.assertj_core)
    testImplementation("io.mockk:mockk:1.9.3")
}
