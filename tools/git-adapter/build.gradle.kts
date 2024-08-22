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
    commonMainApi("org.jetbrains.kotlinx:kotlinx-datetime")
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
    commonTestImplementation(project(":git-test"))
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
