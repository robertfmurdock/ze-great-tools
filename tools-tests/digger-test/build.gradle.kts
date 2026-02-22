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
    sourceSets.all { languageSettings.optIn("kotlin.time.ExperimentalTime") }
}

dependencies {
    commonMainImplementation("com.zegreatrob.tools:digger-core")
    commonMainImplementation("com.zegreatrob.tools:digger-json")
    commonMainImplementation(libs.com.zegreatrob.testmints.standard)
    commonMainImplementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.test)
    commonMainApi("com.zegreatrob.tools:git-test")
    commonMainApi(libs.org.jetbrains.kotlin.kotlin.test)
    "jvmMainImplementation"(libs.org.jetbrains.kotlin.kotlin.test.junit5)
    "jvmMainImplementation"("org.junit.jupiter:junit-jupiter-api")
    "jvmMainImplementation"("org.junit.jupiter:junit-jupiter-engine")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
