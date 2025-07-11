@file:Suppress("UnstableApiUsage")

plugins {
    id("com.zegreatrob.tools.plugins.library")
    id("org.jetbrains.kotlin.plugin.serialization") version embeddedKotlinVersion
}

group = "com.zegreatrob.tools"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) { nodejs() }
}

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainApi(project(":tagger-core"))
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    commonTestImplementation(kotlin("test"))
    "commonTestImplementation"("org.jetbrains.kotlin:kotlin-stdlib") {
        version {
            strictly(embeddedKotlinVersion)
        }
    }
    "jvmTestImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
}

tasks {
    named<Test>("jvmTest") {
        useJUnitPlatform()
    }
    formatKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    lintKotlinCommonMain {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}
