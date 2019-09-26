import de.fayard.BuildSrcVersionsTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Libs.com_android_tools_build_gradle)
        classpath(Libs.kotlin_gradle_plugin)
        classpath(Libs.android_maven_publish)
        classpath(Libs.dokka_gradle_plugin)
        classpath(Libs.dokka_android_gradle_plugin)
        classpath("com.dicedmelon.gradle:jacoco-android:0.1.4")
    }
}

plugins {
    buildSrcVersions
    `maven-publish`
    id("com.diffplug.gradle.spotless") version (Versions.com_diffplug_gradle_spotless_gradle_plugin)
}

subprojects {
    group = "com.ivoberger.jmusicbot-client"
    version = "0.9.0"

    repositories {
        google()
        jcenter()
    }

    tasks.withType<KotlinCompile> { kotlinOptions { jvmTarget = "1.8" } }

    afterEvaluate {
        tasks.named<DokkaTask>("dokka") {
            outputFormat = "html"
            outputDirectory = "$buildDir/javadoc"
        }
        dependencies {
            "implementation"(Libs.kotlin_logging)
        }
    }

    tasks.register<Jar>("javadocJar") {
        val dokka = tasks.named<DokkaTask>("dokka")
        archiveClassifier.set("javadoc")
        from(dokka.get().outputDirectory)
        dependsOn(dokka)
    }
}

publishing {
    repositories {
        maven {
            val githubUsername = System.getenv("GITHUB_USERNAME")
            name = "github"
            url = uri("https://maven.pkg.github.com/$githubUsername")
            credentials {
                username = githubUsername
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

spotless {
    kotlin {
        target("**/kotlin/**/*.kt")
        targetExclude("buildSrc/**")
        ktlint()
        licenseHeaderFile("licenseHeader.txt")
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
    gradleVersion = Versions.gradleLatestVersion
}

tasks.named<BuildSrcVersionsTask>("buildSrcVersions") {
    finalizedBy(tasks.wrapper)
}
