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
    id("de.fayard.buildSrcVersions") version (Versions.de_fayard_buildsrcversions_gradle_plugin)
    id("com.diffplug.gradle.spotless") version (Versions.com_diffplug_gradle_spotless_gradle_plugin)
}

allprojects {
    version = "0.8.0"
    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

spotless {
    kotlin {
        target("**/java/**/*.kt")
        ktlint()
        licenseHeader(
            "/*\n" +
                    "* Copyright 2019 Ivo Berger\n" +
                    "*\n" +
                    "* Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "* you may not use this file except in compliance with the License.\n" +
                    "* You may obtain a copy of the License at\n" +
                    "*\n" +
                    "* http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "*\n" +
                    "* Unless required by applicable law or agreed to in writing, software\n" +
                    "* distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "* See the License for the specific language governing permissions and\n" +
                    "* limitations under the License.\n" +
                    "*/"
        )
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktlint()
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.BIN
    version = Versions.Gradle.runningVersion
}
