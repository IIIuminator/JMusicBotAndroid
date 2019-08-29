import de.fayard.BuildSrcVersionsTask

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Libs.com_android_tools_build_gradle)
        classpath(Libs.kotlin_gradle_plugin)
        classpath(Libs.com_github_dcendents_android_maven_gradle_plugin)
    }
}

plugins {
    buildSrcVersions
    id("com.diffplug.gradle.spotless") version (Versions.com_diffplug_gradle_spotless_gradle_plugin)
}

allprojects {
    group = "com.ivoberger.jmusicbot-client"
    version = "0.8.4"
    repositories {
        google()
        jcenter()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
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
