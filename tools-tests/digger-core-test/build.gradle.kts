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
    commonTestImplementation("com.zegreatrob.tools:digger-core")
    commonTestImplementation(libs.testmints.minassert)
    commonTestImplementation(libs.testmints.standard)
    commonTestImplementation(libs.org.jetbrains.kotlin.kotlin.test)
    "jvmTestImplementation"(libs.org.jetbrains.kotlin.kotlin.test.junit5)
    "jvmTestImplementation"("com.zegreatrob.tools:git-test")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
        environment("GIT_CONFIG_COUNT", "0")
        environment("GIT_CONFIG_PARAMETERS", "")
    }
}
