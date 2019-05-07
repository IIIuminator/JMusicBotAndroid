buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Libs.com_android_tools_build_gradle)
        classpath(Libs.kotlin_gradle_plugin)
    }
}

plugins {
    id("com.github.dcendents.android-maven") version Versions.com_github_dcendents_android_maven_gradle_plugin
    id("de.fayard.buildSrcVersions") version Versions.de_fayard_buildsrcversions_gradle_plugin
}

allprojects {
    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

tasks.wrapper{
    distributionType = Wrapper.DistributionType.BIN
    version = Versions.Gradle.runningVersion
}
