plugins {
    id("com.zegreatrob.tools.plugins.mp")
}

kotlin {
    jvm()
    js(IR) { nodejs() }
    sourceSets.all { languageSettings.optIn("kotlin.time.ExperimentalTime") }
}

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainImplementation(project(":digger-core"))
    commonMainImplementation(project(":digger-json"))
    commonMainApi(project(":git-test"))
    commonMainApi(kotlin("test", embeddedKotlinVersion))
    "jvmMainImplementation"(kotlin("test-junit5", embeddedKotlinVersion))
    "jvmMainImplementation"("org.junit.jupiter:junit-jupiter-api")
    "jvmMainImplementation"("org.junit.jupiter:junit-jupiter-engine")
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
    }
}
