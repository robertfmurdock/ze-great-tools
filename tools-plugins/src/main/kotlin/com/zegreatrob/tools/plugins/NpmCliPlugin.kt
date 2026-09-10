package com.zegreatrob.tools.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

abstract class NpmCliExtension {
    abstract val packageName: Property<String>
    abstract val description: Property<String>
    abstract val binaryName: Property<String>
    abstract val binaryPath: Property<String>
    abstract val keywords: ListProperty<String>
    abstract val directory: Property<String>
    abstract val author: Property<String>
    abstract val license: Property<String>
    abstract val homepage: Property<String>
    abstract val repositoryUrl: Property<String>
    abstract val bugsUrl: Property<String>
    abstract val nodeEngine: Property<String>
    abstract val files: ListProperty<String>
    abstract val guideResourcesDir: DirectoryProperty
    abstract val guideFile: Property<String>

    init {
        author.convention("rob@continuousexcellence.io")
        license.convention("MIT")
        homepage.convention("https://github.com/robertfmurdock/ze-great-tools")
        repositoryUrl.convention("https://github.com/robertfmurdock/ze-great-tools.git")
        bugsUrl.convention("https://github.com/robertfmurdock/ze-great-tools/issues")
        nodeEngine.convention(">=18.0.0")
        files.convention(listOf("kotlin", "README.md", "LICENSE"))
    }
}

class NpmCliPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val npmCli = project.extensions.create<NpmCliExtension>("npmCli")

        project.afterEvaluate {
            val kotlin = project.extensions.findByType<KotlinMultiplatformExtension>() ?: return@afterEvaluate
            val mainCompilation = kotlin.js().compilations.getByName("main")
            val mainNpmProjectDir = mainCompilation.npmProject.dir

            mainCompilation.packageJson {
                if (npmCli.packageName.isPresent) {
                    name = npmCli.packageName.get()
                    customField("package-name", npmCli.packageName.get())
                }
                if (npmCli.description.isPresent) {
                    customField("description", npmCli.description.get())
                }
                if (npmCli.author.isPresent) {
                    customField("author", npmCli.author.get())
                }
                if (npmCli.license.isPresent) {
                    customField("license", npmCli.license.get())
                }
                if (npmCli.keywords.isPresent) {
                    customField("keywords", npmCli.keywords.get().toTypedArray())
                }
                if (npmCli.binaryName.isPresent) {
                    val binPath = npmCli.binaryPath.orNull ?: "kotlin/bin/${npmCli.binaryName.get()}"
                    customField("bin", mapOf(npmCli.binaryName.get() to binPath))
                }
                if (npmCli.homepage.isPresent) {
                    customField("homepage", npmCli.homepage.get())
                }
                if (npmCli.repositoryUrl.isPresent && npmCli.directory.isPresent) {
                    customField(
                        "repository",
                        mapOf(
                            "type" to "git",
                            "url" to npmCli.repositoryUrl.get(),
                            "directory" to npmCli.directory.get(),
                        ),
                    )
                }
                if (npmCli.bugsUrl.isPresent) {
                    customField("bugs", mapOf("url" to npmCli.bugsUrl.get()))
                }
                if (npmCli.nodeEngine.isPresent) {
                    customField("engines", mapOf("node" to npmCli.nodeEngine.get()))
                }
                if (npmCli.files.isPresent) {
                    customField("files", npmCli.files.get().toTypedArray())
                }
            }

            if (npmCli.guideResourcesDir.isPresent) {
                val copyGuideResources = project.tasks.register<Copy>("copyGuideResources") {
                    group = "build"
                    description = "Copy guide resources into generated resources directory"
                    from(npmCli.guideResourcesDir)
                    into(project.layout.buildDirectory.dir("generated/resources/commonMain"))
                    if (npmCli.guideFile.isPresent) {
                        include(npmCli.guideFile.get())
                    }
                }
                project.tasks.withType<ProcessResources>().configureEach {
                    dependsOn(copyGuideResources)
                }
                kotlin.sourceSets.named("commonMain").configure {
                    resources.srcDir(copyGuideResources.map { it.destinationDir })
                }
            }

            val copyReadme = project.tasks.register<Copy>("copyReadme") {
                dependsOn("jsPackageJson", ":kotlinNpmInstall")
                from(project.layout.projectDirectory.file("README.md"))
                into(mainNpmProjectDir)
            }

            val rootLicense = project.rootProject.layout.projectDirectory.file("LICENSE")
            val parentLicense = project.rootProject.layout.projectDirectory.file("../LICENSE")
            val licenseFile = if (rootLicense.asFile.exists()) rootLicense else parentLicense

            val copyLicense = project.tasks.register<Copy>("copyLicense") {
                dependsOn("jsPackageJson", ":kotlinNpmInstall")
                from(licenseFile)
                into(mainNpmProjectDir)
            }

            val jsCliTar = project.tasks.register<Tar>("jsCliTar") {
                dependsOn(
                    copyReadme,
                    copyLicense,
                    "jsPackageJson",
                    ":kotlinNpmInstall",
                    "compileKotlinJs",
                    "jsProcessResources",
                    "compileProductionExecutableKotlinJs",
                    "jsProductionExecutableCompileSync",
                )
                from(mainNpmProjectDir)
                compression = Compression.GZIP
                archiveFileName.set("${project.name}-js.tgz")
            }

            project.tasks.register<Exec>("jsLink") {
                group = "build setup"
                description = "Link CLI to local npm for development testing"
                dependsOn(jsCliTar)
                workingDir(mainNpmProjectDir)
                commandLine("npm", "link")
            }

            val jsPublish = project.tasks.register<Exec>("jsPublish") {
                dependsOn(jsCliTar)
                mustRunAfter(project.tasks.named("check"))
                workingDir(mainNpmProjectDir)
                doFirst {
                    if (project.isSnapshot()) {
                        commandLine("npm", "publish", "--dry-run", "--access", "public", "--tag", "snapshot")
                    } else {
                        commandLine("npm", "publish", "--access", "public", "--provenance")
                    }
                }
            }

            project.tasks.register("publish") {
                group = "publishing"
                description = "Publish CLI to npm registry"
                dependsOn(jsPublish)
                mustRunAfter(project.tasks.named("check"))
            }
        }
    }

    private fun Project.isSnapshot() = version.toString().contains("SNAPSHOT")
}
