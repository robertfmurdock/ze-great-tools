@file:Suppress("UnstableApiUsage")

plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
}

testing {
    suites {
        register<JvmTestSuite>("functionalTest")
    }
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

dependencies {
    testImplementation(libs.com.zegreatrob.testmints.minassert)
    testImplementation(libs.com.zegreatrob.testmints.standard)
    testImplementation(libs.org.jetbrains.kotlin.kotlin.test.junit5)
    testImplementation("com.zegreatrob.tools:tagger-gradle")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("com.zegreatrob.tools:tagger-plugin")

    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    "functionalTestImplementation"("com.zegreatrob.tools:tagger-test")
}

tasks {
    "functionalTest" {
        dependsOn(gradle.includedBuild("tools").task(":tagger-plugin:assemble"))
    }
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }
    withType<Test> {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
}
