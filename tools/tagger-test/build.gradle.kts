plugins {
    id("com.zegreatrob.tools.plugins.mp")
}

kotlin {
    jvm { withJava() }
    js(IR) { nodejs() }
}

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainImplementation(project(":tagger-core"))
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
