@file:Suppress("UnstableApiUsage")

plugins {
    id("com.zegreatrob.tools.plugins.mp")
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
    commonMainApi(platform(project(":dependency-bom")))
    commonTestImplementation(kotlin("test"))
    "jvmTestImplementation"(kotlin("test-junit5"))
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
