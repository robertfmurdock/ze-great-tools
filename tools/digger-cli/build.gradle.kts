plugins {
    application
    id("com.zegreatrob.tools.plugins.mp")
}

kotlin {
    jvm { withJava() }
    js(IR) { nodejs() }
}

application {
    mainClass.set("com.zegreatrob.tools.digger.cli.MainKt")
}

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainImplementation(project(":digger-core"))
    commonMainImplementation(project(":digger-json"))
    commonMainImplementation("com.github.ajalt.clikt:clikt")
    commonTestImplementation(kotlin("test", embeddedKotlinVersion))
}

tasks {
    withType<CreateStartScripts> {
        applicationName = "digger"
    }
}
