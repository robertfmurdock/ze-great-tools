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
    commonMainImplementation(kotlin("stdlib", embeddedKotlinVersion))
    commonMainImplementation(kotlin("test"))
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    commonMainApi("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}
