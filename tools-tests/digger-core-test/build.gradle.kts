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
    commonTestImplementation("com.zegreatrob.tools:digger-core")
    commonTestImplementation(libs.com.zegreatrob.testmints.standard)
    commonTestImplementation(libs.org.jetbrains.kotlin.kotlin.test)
    "jvmTestImplementation"(libs.org.jetbrains.kotlin.kotlin.test.junit5)
    "jvmTestImplementation"(libs.com.zegreatrob.testmints.standard)
    "jvmTestImplementation"("com.zegreatrob.tools:git-test")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
}
