@file:Suppress("UnstableApiUsage")

plugins {
    id("com.zegreatrob.tools.plugins.mp")
    id("com.zegreatrob.tools.plugins.publish")
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
    "jvmTestImplementation"(libs.org.jetbrains.kotlin.kotlin.test.junit5)
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
