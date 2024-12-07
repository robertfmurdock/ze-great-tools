@file:Suppress("UnstableApiUsage")

plugins {
    id("com.zegreatrob.tools.plugins.library")
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
    "jvmTestImplementation"(kotlin("test-junit", embeddedKotlinVersion))
}

tasks {
//    formatKotlinCommonMain {
//        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
//    }
//    lintKotlinCommonMain {
//        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
//    }
}
