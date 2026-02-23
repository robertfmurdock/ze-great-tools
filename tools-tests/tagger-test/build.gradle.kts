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
    commonMainImplementation(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    commonMainImplementation("com.zegreatrob.tools:tagger-json")
    commonMainImplementation("com.zegreatrob.tools:tagger-core")
    commonMainImplementation("com.zegreatrob.tools:cli-tools")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    commonMainImplementation(libs.com.zegreatrob.testmints.minassert)
    commonMainImplementation(libs.com.zegreatrob.testmints.standard)
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
