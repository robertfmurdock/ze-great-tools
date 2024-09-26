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
    commonMainApi(project(":git-adapter"))
    commonMainImplementation(project(":digger-core"))
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
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
