plugins {
    id("com.zegreatrob.tools.plugins.test-suite")
    id("org.jetbrains.kotlin.multiplatform")
}

group = "com.zegreatrob.tools"

kotlin {
    jvm()
    js { nodejs() }
}

dependencies {
    commonTestImplementation("com.zegreatrob.tools:tagger-core")
    commonTestImplementation(libs.testmints.minassert)
    commonTestImplementation(libs.testmints.standard)
    commonTestImplementation(libs.org.jetbrains.kotlin.kotlin.test)
    "jvmTestImplementation"(libs.org.jetbrains.kotlin.kotlin.test.junit5)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
