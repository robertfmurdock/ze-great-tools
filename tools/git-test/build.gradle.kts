plugins {
    id("com.zegreatrob.tools.plugins.mp")
}

kotlin {
    jvm { withJava() }
    js(IR) { nodejs() }
}

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainImplementation(project(":git-adapter"))
    "jvmMainApi"(kotlin("test-junit5", embeddedKotlinVersion))
    "jvmMainApi"("org.junit.jupiter:junit-jupiter-api")
    "jvmMainApi"("org.junit.jupiter:junit-jupiter-engine")
}
