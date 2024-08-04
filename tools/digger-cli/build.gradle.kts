import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    application
    id("com.zegreatrob.tools.plugins.mp")
}

kotlin {
    jvm { withJava() }
    js(IR) {
        nodejs {
            useCommonJs()
            binaries.executable()
        }
        compilations {
            "main" {
                packageJson {
                    customField("bin", mapOf("digger" to "./kotlin/bin/digger"))
                }
            }
        }
    }
}

application {
    mainClass.set("com.zegreatrob.tools.digger.cli.MainKt")
}

val mainNpmProjectDir = kotlin.js().compilations.getByName("main").npmProject.dir

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainImplementation(project(":digger-core"))
    commonMainImplementation(project(":digger-json"))
    commonMainImplementation("com.github.ajalt.clikt:clikt")

    commonTestImplementation(project(":digger-test"))
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
    }

    withType<CreateStartScripts> {
        applicationName = "digger"
    }

    val jsCliTar by registering(Tar::class) {
        dependsOn(
            "jsPackageJson",
            ":kotlinNpmInstall",
            "compileKotlinJs",
            "jsProcessResources",
            "compileProductionExecutableKotlinJs",
            "jsProductionExecutableCompileSync",
        )
        from(mainNpmProjectDir)
        compression = Compression.GZIP
        archiveFileName.set("digger-cli-js.tgz")
    }
    val jsLink by registering(Exec::class) {
        dependsOn(jsCliTar)
        workingDir(mainNpmProjectDir)
        commandLine("npm", "link")
    }
}
