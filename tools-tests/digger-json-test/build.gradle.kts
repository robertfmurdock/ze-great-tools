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
    sourceSets.all { languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi") }
}

dependencies {
    commonTestImplementation("com.zegreatrob.tools:digger-json")
    commonTestImplementation(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    commonTestImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
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
