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
    "jvmMainApi"(libs.org.ajoberstar.grgit.grgit.core)
    "jvmMainApi"(kotlin("test-junit5", embeddedKotlinVersion))
    "jvmMainApi"("org.junit.jupiter:junit-jupiter-api")
    "jvmMainApi"("org.junit.jupiter:junit-jupiter-engine")
}
