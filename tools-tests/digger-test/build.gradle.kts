plugins {
    id("com.zegreatrob.tools.plugins.test-suite")
    id("org.jetbrains.kotlin.multiplatform")
}

group = "com.zegreatrob.tools"

kotlin {
    jvmToolchain(21)
    jvm()
    js { nodejs() }
    sourceSets.all { languageSettings.optIn("kotlin.time.ExperimentalTime") }
}

dependencies {
    commonMainImplementation("com.zegreatrob.tools:digger-core")
    commonMainImplementation("com.zegreatrob.tools:digger-json")
    commonMainImplementation(libs.testmints.async)
    commonMainImplementation(libs.testmints.minassert)
    commonMainImplementation(libs.testmints.standard)
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
