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
    commonMainApi(project(":digger-model"))
    commonMainApi(project(":git-adapter"))
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
    "jvmTestImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
    "jvmTestImplementation"(project(":digger-test"))
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
