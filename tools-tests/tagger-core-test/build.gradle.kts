plugins {
    id("org.jetbrains.kotlin.multiplatform")
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
    commonTestImplementation("com.zegreatrob.tools:tagger-core")
    commonTestImplementation(libs.com.zegreatrob.testmints.minassert)
    commonTestImplementation(libs.com.zegreatrob.testmints.standard)
    commonTestImplementation(libs.org.jetbrains.kotlin.kotlin.test)
    "jvmTestImplementation"(libs.org.jetbrains.kotlin.kotlin.test.junit5)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
