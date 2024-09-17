import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    application
    id("com.zegreatrob.tools.plugins.mp")
    id("org.jetbrains.kotlin.plugin.serialization") version embeddedKotlinVersion
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
                    name = "git-semver-tagger"
                    customField("package-name", "git-semver-tagger")
                    customField("author", "rob@continuousexcellence.io")
                    customField("license", "MIT")
                    customField("keywords", arrayOf("git", "contribution", "pair", "agile", "coaching", "statistics"))
                    customField("bin", mapOf("tagger" to "kotlin/bin/tagger"))
                    customField("homepage", "https://github.com/robertfmurdock/ze-great-tools")
                }
            }
        }
    }
}

application {
    mainClass.set("com.zegreatrob.tools.tagger.cli.MainKt")
}

val mainNpmProjectDir = kotlin.js().compilations.getByName("main").npmProject.dir

dependencies {
    commonMainImplementation(platform(project(":dependency-bom")))
    commonMainImplementation(project(":cli-tools"))
    commonMainImplementation(project(":tagger-core"))
    commonMainImplementation("com.github.ajalt.clikt:clikt")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    commonTestImplementation(project(":tagger-test"))
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    withType<CreateStartScripts> {
        applicationName = "tagger"
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
        archiveFileName.set("tagger-cli-js.tgz")
    }
    val jsLink by registering(Exec::class) {
        dependsOn(jsCliTar)
        workingDir(mainNpmProjectDir)
        commandLine("npm", "link")
    }
    val confirmTaggerCanRun by registering(Exec::class) {
        dependsOn(jsCliTar)
        workingDir(mainNpmProjectDir)
        commandLine("kotlin/bin/tagger", "calculate-version")
    }
    val jsPublish by registering(Exec::class) {
        dependsOn(jsCliTar)
        enabled = !isSnapshot()
        mustRunAfter(check)
        workingDir(mainNpmProjectDir)
        commandLine("npm", "publish")
    }
    check {
        dependsOn(confirmTaggerCanRun)
    }
    publish {
        dependsOn(jsPublish)
        mustRunAfter(check)
    }
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")
