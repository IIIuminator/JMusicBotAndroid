plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    jacoco
    `maven-publish`
}

tasks.test { useJUnitPlatform() }

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>(name) {
            from(components["java"])

            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

dependencies {
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_core)

    implementation(Libs.statemachine)

    implementation(Libs.dagger)
    kapt(Libs.dagger_compiler)

    implementation(Libs.java_jwt)
    implementation(Libs.okhttp)
    implementation(Libs.logging_interceptor)
    implementation(Libs.retrofit)
    implementation(Libs.converter_moshi)
    implementation(Libs.moshi)
    kapt(Libs.moshi_kotlin_codegen)

    testImplementation(Libs.junit_jupiter)
    testImplementation(Libs.kotlinx_coroutines_test)
    testImplementation(Libs.mockwebserver)
    testRuntime(Libs.slf4j_simple)
}
