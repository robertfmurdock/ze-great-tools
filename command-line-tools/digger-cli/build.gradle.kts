
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.org.jmailen.kotlinter)
    alias(libs.plugins.com.github.ben.manes.versions)
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
        }
        compilations {
            "main" {
                packageJson {
                    name = "git-digger"
                    customField("package-name", "git-digger")
                    customField("author", "rob@continuousexcellence.io")
                    customField("license", "MIT")
                    customField("keywords", arrayOf("git", "contribution", "pair", "agile", "coaching", "statistics"))
                    customField("bin", mapOf("digger" to "kotlin/bin/digger"))
                    customField("homepage", "https://github.com/robertfmurdock/ze-great-tools")
                }
            }
        }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

val mainNpmProjectDir = kotlin.js().compilations.getByName("main").npmProject.dir

dependencies {
    commonMainImplementation("com.zegreatrob.tools:cli-tools:$version")
    commonMainImplementation("com.zegreatrob.tools:digger-core:$version")
    commonMainImplementation("com.zegreatrob.tools:digger-json:$version")
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt)

    commonTestImplementation("com.zegreatrob.tools:digger-test:$version")
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
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
    val jsPublish by registering(Exec::class) {
        dependsOn(jsCliTar)
        enabled = !isSnapshot()
        mustRunAfter(check)
        workingDir(mainNpmProjectDir)
        commandLine("npm", "publish")
    }
    val publish by creating {
        dependsOn(jsPublish)
        mustRunAfter(check)
    }
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")
