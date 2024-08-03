plugins {
    id("com.zegreatrob.tools.plugins.mp")
}

kotlin {
    jvm { withJava() }
    js(IR) { nodejs() }
}

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainImplementation(project(":digger-core"))
    commonMainImplementation(project(":digger-json"))
    "jvmMainApi"(libs.org.ajoberstar.grgit.grgit.core)
}
