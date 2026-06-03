@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version embeddedKotlinVersion
    alias(libs.plugins.org.jmailen.kotlinter)
}

repositories {
    mavenCentral()
}

val generatedDirectory = project.layout.buildDirectory.dir("generated-sources/templates/kotlin/main")

kotlin {
    jvm {
        binaries {
            executable {
                mainClass.set("com.zegreatrob.coupling.cli.MainKt")
            }
        }
    }
    js(IR) {
        nodejs {
            useCommonJs()
            binaries.executable()
            testTask {
                useMocha { timeout = "10s" }
                environment("EXPECTED_VERSION", "${project.version}")
                environment("GIT_CONFIG_GLOBAL", "/dev/null")
                environment("GIT_CONFIG_SYSTEM", "/dev/null")
            }
        }
        compilations {
            "main" {
                packageJson {
                    name = "@continuous-excellence/tagger"
                    customField("package-name", "@continuous-excellence/tagger")
                    customField("author", "rob@continuousexcellence.io")
                    customField("license", "MIT")
                    customField("keywords", arrayOf("git", "contribution", "pair", "agile", "coaching", "statistics"))
                    customField("bin", mapOf("tagger" to "kotlin/bin/tagger"))
                    customField("homepage", "https://github.com/robertfmurdock/ze-great-tools")
                    customField("repository", "github:robertfmurdock/ze-great-tools")
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
    commonMainImplementation(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    commonMainImplementation("com.zegreatrob.tools:cli-tools")
    commonMainImplementation("com.zegreatrob.tools:tagger-json")
    commonMainImplementation("com.zegreatrob.tools:tagger-core")
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt)
    commonMainImplementation(libs.com.github.ajalt.clikt.clikt.markdown)
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    commonTestImplementation("com.zegreatrob.tools:tagger-test")
    commonTestImplementation(kotlin("test"))
    commonTestImplementation(libs.com.zegreatrob.testmints.minassert)
    commonTestImplementation(libs.com.zegreatrob.testmints.standard)
}

tasks {
    withType(Test::class) {
        useJUnitPlatform()
        environment("EXPECTED_VERSION", project.version)
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
    }
    withType<CreateStartScripts> {
        applicationName = "tagger"
    }
    val copyReadme by registering(Copy::class) {
        dependsOn("jsPackageJson", ":kotlinNpmInstall")
        from(layout.projectDirectory.file("README.md"))
        into(mainNpmProjectDir)
    }
    val copyHelpResources by registering(Copy::class) {
        dependsOn("jsProcessResources", "jsPackageJson", ":kotlinNpmInstall")
        from(layout.buildDirectory.dir("processedResources/js/main"))
        into(mainNpmProjectDir)
    }
    val jsCliTar by registering(Tar::class) {
        dependsOn(
            copyReadme,
            copyHelpResources,
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
    register("jsLink", Exec::class) {
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
        mustRunAfter(check)
        workingDir(mainNpmProjectDir)
        if (isSnapshot()) {
            commandLine("npm", "publish", "--dry-run", "--access", "public")
        } else {
            commandLine("npm", "publish", "--access", "public")
        }
    }
    check {
        dependsOn(confirmTaggerCanRun)
    }
    register("publish") {
        dependsOn(jsPublish)
        mustRunAfter(check)
    }
    val copyTemplates by registering(Copy::class) {
        inputs.property("version", rootProject.version)
        filteringCharset = "UTF-8"
        from(project.projectDir.resolve("src/commonMain/templates")) {
            filter<ReplaceTokens>("tokens" to mapOf("TAGGER_VERSION" to rootProject.version))
        }
        into(generatedDirectory)
    }
    withType<KotlinCompile> {
        dependsOn(copyTemplates)
    }
    kotlin.sourceSets {
        commonMain { kotlin.srcDir(copyTemplates) }
    }
}

fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")
