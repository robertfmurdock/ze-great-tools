import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jmailen.kotlinter)
    alias(libs.plugins.com.github.ben.manes.versions)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    jvm { withJava() }
    js(IR) {
        nodejs {
            useCommonJs()
            binaries.executable()
            testTask {
                useMocha { timeout = "10s" }
                environment("GIT_CONFIG_GLOBAL", "/dev/null")
                environment("GIT_CONFIG_SYSTEM", "/dev/null")
            }
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

val mainNpmProjectDir = kotlin.js().compilations.getByName("main").npmProject.dir

dependencies {
    commonMainImplementation(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    commonMainImplementation("com.zegreatrob.tools:cli-tools")
    commonMainImplementation("com.zegreatrob.tools:tagger-core")
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt)
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    commonTestImplementation("com.zegreatrob.tools:tagger-test")
    commonTestImplementation(kotlin("test"))
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
    val publish by creating {
        dependsOn(jsPublish)
        mustRunAfter(check)
    }
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")