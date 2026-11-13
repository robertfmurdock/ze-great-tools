package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

class AdditionalTasksFunctionalTest {
    private open class TemporaryProject : AutoCloseable {
        val projectDir = createTempDirectory()

        override fun close() {
            removeDirectory(projectDir)
        }
    }

    private data class ConfigurationCacheRun(
        val first: BuildResult,
        val reused: BuildResult,
        val firstArguments: List<String>,
    )

    private data class PublicationRetryRun(
        val firstAttempt: BuildResult,
        val retry: BuildResult,
    )

    @Test
    fun generatedNonDraftSettingSkipsDraftPublication() = publicationWithGeneratedDraftSetting(
        false,
        "SKIPPED",
        emptyList(),
    )

    @Test
    fun generatedDraftSettingEnablesDraftPublication() = publicationWithGeneratedDraftSetting(
        true,
        "SUCCESS",
        listOf("release", "edit", "1.2.3", "--draft=false"),
    )

    private fun publicationWithGeneratedDraftSetting(
        draft: Boolean,
        expectedOutcome: String,
        expectedArguments: List<String>,
    ) = setup(object : TemporaryProject() {
        val output = File(projectDir, "gh-called.txt")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                if [ "$2" = "view" ]; then echo true; exit 0; fi
                printf '%s\n' "$@" > "${output.absolutePath}"
                """.trimIndent(),
            )
            setExecutable(true)
        }
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.tagger") }
            val setting = layout.buildDirectory.file("draft-setting.txt")
            val generateSetting = tasks.register<WriteProperties>("generateDraftSetting") {
                destinationFile.set(setting)
                property("draft", "$draft")
            }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseDraft.set(providers.fileContents(setting).asText.map { it.contains("draft=true") })
            }
            tasks.named<Exec>("githubReleasePublish") {
                dependsOn(generateSetting)
                environment("PATH", "$projectDir/bin:" + System.getenv("PATH"))
            }
            """.trimIndent(),
        )
    } exercise {
        GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("githubReleasePublish", "-x", "githubReleaseUpload", "-Pversion=1.2.3", "--configuration-cache", "--console=plain")
            .build()
    } verify { result ->
        result.task(":generateDraftSetting")?.outcome?.name.assertIsEqualTo("SUCCESS")
        result.task(":githubReleasePublish")?.outcome?.name.assertIsEqualTo(expectedOutcome)
        (output.takeIf { it.exists() }?.readLines() ?: emptyList()).assertIsEqualTo(expectedArguments)
    }

    @Test
    fun generatedDraftSettingControlsReleaseCreation() = setup(object : TemporaryProject() {
        val output = File(projectDir, "gh-create-arguments.txt")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                if [ "$2" = "view" ]; then echo "release not found" >&2; exit 1; fi
                printf '%s\n' "$@" >> "${output.absolutePath}"
                """.trimIndent(),
            )
            setExecutable(true)
        }
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.tagger") }
            val setting = layout.buildDirectory.file("draft-setting.txt")
            val generateSetting = tasks.register<WriteProperties>("generateDraftSetting") {
                destinationFile.set(setting)
                property("draft", "true")
            }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseDraft.set(providers.fileContents(setting).asText.map { it.contains("draft=true") })
            }
            tasks.named<Exec>("githubRelease") {
                dependsOn(generateSetting)
                environment("PATH", "$projectDir/bin:" + System.getenv("PATH"))
            }
            """.trimIndent(),
        )
    } exercise {
        val runner = GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("githubRelease", "-x", "tag", "-Pversion=1.2.3", "--configuration-cache", "--console=plain")
        val first = runner.build()
        val firstArguments = output.takeIf { it.exists() }?.readLines().orEmpty()
        ConfigurationCacheRun(first, runner.build(), firstArguments)
    } verify { (first, second, firstArguments) ->
        first.task(":generateDraftSetting")?.outcome?.name.assertIsEqualTo("SUCCESS")
        second.output.contains("Reusing configuration cache.").assertIsEqualTo(true)
        val expectedArguments = listOf("release", "create", "1.2.3", "--verify-tag", "--draft", "--title", "1.2.3", "--notes", "1.2.3")
        firstArguments.assertIsEqualTo(expectedArguments)
        output.takeIf { it.exists() }?.readLines().orEmpty().drop(firstArguments.size)
            .assertIsEqualTo(expectedArguments)
    }

    @Test
    fun uploadSkipsWhenNoReleaseAssetsExist() = setup(TemporaryProject()) {
        writeSettings(projectDir)
        File(projectDir, "distributions").apply { mkdirs() }
            .resolve("unmatched.txt").writeText("unmatched")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.tagger") }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseAssets.from(fileTree("distributions") { include("*.zip") })
            }
            """.trimIndent(),
        )
    } exercise {
        GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("githubReleaseUpload", "-x", "githubRelease", "-Pversion=1.2.3", "--console=plain")
            .build()
    } verify { result ->
        result.task(":validateGithubReleaseAssets")?.outcome?.name.assertIsEqualTo("SUCCESS")
        result.output.contains("No GitHub release assets configured").assertIsEqualTo(true, result.output)
        result.task(":githubReleaseUpload")?.outcome?.name.assertIsEqualTo("SKIPPED")
    }

    @Test
    fun generatedReleaseAssetIsUploaded() = setup(object : TemporaryProject() {
        val output = File(projectDir, "gh-arguments.txt")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                if [ "$2" = "view" ]; then exit 1; fi
                printf '%s\n' "$@" >> "${output.absolutePath}"
                """.trimIndent(),
            )
            setExecutable(true)
        }
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.tagger") }
            val generateAsset = tasks.register("generateReleaseAsset") {
                val asset = layout.buildDirectory.file("assets/generated asset.zip")
                outputs.file(asset)
                doLast {
                    asset.get().asFile.apply {
                        parentFile.mkdirs()
                        writeText("generated asset")
                    }
                }
            }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseAssets.from(files(fileTree("build/assets")).builtBy(generateAsset))
            }
            tasks.named<Exec>("githubReleaseUpload") {
                environment("PATH", "$projectDir/bin:" + System.getenv("PATH"))
            }
            """.trimIndent(),
        )
    } exercise {
        val runner = GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("githubReleaseUpload", "-x", "githubRelease", "-Pversion=1.2.3", "--configuration-cache", "--console=plain")
        val first = runner.build()
        val firstArguments = output.takeIf { it.exists() }?.readLines().orEmpty()
        ConfigurationCacheRun(first, runner.build(), firstArguments)
    } verify { (first, second, firstArguments) ->
        first.task(":generateReleaseAsset")?.outcome?.name.assertIsEqualTo("SUCCESS")
        first.task(":githubReleaseUpload")?.outcome?.name.assertIsEqualTo("SUCCESS")
        second.output.contains("Reusing configuration cache.").assertIsEqualTo(true)
        val expectedArguments = listOf("release", "upload", "1.2.3", "build/assets/generated asset.zip")
        firstArguments.assertIsEqualTo(expectedArguments)
        output.takeIf { it.exists() }?.readLines().orEmpty().drop(firstArguments.size)
            .assertIsEqualTo(expectedArguments)
    }

    @Test
    fun githubReleaseWaitsForDistributionsAndRunsAfterPublicationRetry() = setup(object : TemporaryProject() {
        val events = File(projectDir, "events.txt")
        val createArguments = File(projectDir, "create-arguments")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                if [ "$2" = "view" ]; then echo "release not found" >&2; exit 1; fi
                printf '%s\n' "$@" > "${createArguments.absolutePath}"
                printf 'github\n' >> "${events.absolutePath}"
                """.trimIndent(),
            )
            setExecutable(true)
        }
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.tagger") }
            version = "1.2.3"
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
            }
            val failedOnce = layout.projectDirectory.file("publication-failed-once").asFile
            val events = layout.projectDirectory.file("events.txt").asFile
            val maven = tasks.register("closeAndReleaseSonatypeStagingRepository") {
                doLast {
                    if (!failedOnce.exists()) {
                        failedOnce.writeText("failed")
                        events.appendText("maven-failure\n")
                        throw GradleException("simulated Maven publication failure")
                    }
                    events.appendText("maven\n")
                }
            }
            val npm = tasks.register("jsPublish") {
                mustRunAfter(maven)
                doLast { events.appendText("npm\n") }
            }
            val portal = tasks.register("publishPlugins") {
                mustRunAfter(npm)
                doLast { events.appendText("portal\n") }
            }
            tasks.register("mockRelease") {
                dependsOn(maven, npm, portal)
                finalizedBy("githubRelease")
            }
            tasks.named<Exec>("githubRelease") {
                environment("PATH", "${bin.absolutePath}:" + System.getenv("PATH"))
            }
            """.trimIndent(),
        )
    } exercise {
        val runner = GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("mockRelease", "-x", "tag", "--console=plain")
        val firstAttempt = runner.buildAndFail()
        val retry = runner.build()
        PublicationRetryRun(firstAttempt, retry)
    } verify { (firstAttempt, retry) ->
        firstAttempt.output.contains("simulated Maven publication failure")
            .assertIsEqualTo(true, "Expected the first attempt to fail at the simulated Maven publication")
        retry.task(":githubRelease")?.outcome?.name.assertIsEqualTo("SUCCESS")
        createArguments.readLines().assertIsEqualTo(listOf("release", "create", "1.2.3", "--verify-tag", "--title", "1.2.3", "--notes", "1.2.3"))
        events.readLines().assertIsEqualTo(listOf("maven-failure", "maven", "npm", "portal", "github"))
    }

    @Test
    fun versionAndReportTasksReadTheConfiguredRepository() = setup(object : TemporaryProject() {
        val initialTag = "1.2.3"
    }) {
        writeSettings(projectDir)
        writeBuildFile(projectDir)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore"),
            initialTag = initialTag,
            commits = listOf("init", "[patch] commit 1"),
        )
    } exercise {
        GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("previousVersion", "commitReport", "-q", "--console=plain")
            .build()
    } verify { result ->
        result.task(":previousVersion")?.outcome?.name.assertIsEqualTo("SUCCESS")
        result.task(":commitReport")?.outcome?.name.assertIsEqualTo("SUCCESS")
        val lines = result.output.trim().lines()
        lines.contains(initialTag).assertIsEqualTo(true, result.output)
        lines.contains("COMMIT REPORT-------").assertIsEqualTo(true, result.output)
        lines.contains("COMMIT REPORT OVAH--").assertIsEqualTo(true, result.output)
    }

    @Test
    fun releaseDryRunAvoidsImplicitGitHeadDependency() = setup(object : TemporaryProject() {
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
    }) {
        writeSettings(projectDir)
        writeBuildFileWithDigger(projectDir)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init"),
        )
    } exercise {
        GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("release", "--dry-run", "--warning-mode=all", "--console=plain")
            .build()
            .output
    } verify { output ->
        output.contains("implicit dependency")
            .assertIsEqualTo(false, "Unexpected implicit dependency warning.\n$output")
        output.contains("validation_problems")
            .assertIsEqualTo(false, "Unexpected validation problems output.\n$output")
    }

    private fun writeSettings(projectDir: String) {
        File("$projectDir/settings.gradle").writeText(
            "includeBuild(\"${System.getProperty("user.dir")}/../../tools\")",
        )
    }

    private fun writeBuildFile(projectDir: String) {
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
            }
            """.trimIndent(),
        )
    }

    private fun writeBuildFileWithDigger(projectDir: String) {
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
                id("com.zegreatrob.tools.digger")
            }
            version = "1.2.3"
            tagger {
                releaseBranch = "master"
            }
            """.trimIndent(),
        )
    }

    private fun writeGitIgnore(projectDir: String) {
        File("$projectDir/.gitignore").writeText(".gradle")
    }

    @Test
    fun missingReleaseAssetBlocksUpload() = setup(object : TemporaryProject() {
        val missingAsset = File(projectDir, "missing.zip")
        val invocations = File(projectDir, "gh-invocations.txt")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                printf '%s\n' "$*" >> "${invocations.absolutePath}"
                """.trimIndent(),
            )
            setExecutable(true)
        }
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.tagger") }
            tagger {
                githubReleaseEnabled.set(true)
                githubReleaseAssets.from(file("${missingAsset.name}"))
            }
            tasks.named<Exec>("githubReleaseUpload") {
                environment("PATH", "$projectDir/bin:" + System.getenv("PATH"))
            }
            """.trimIndent(),
        )
    } exercise {
        val runner = GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("githubReleaseUpload", "-x", "githubRelease", "-Pversion=1.2.3", "--console=plain")
        runCatching { runner.buildAndFail() }
    } verify { build ->
        val result = build.getOrThrow()
        result.task(":validateGithubReleaseAssets")?.outcome?.name.assertIsEqualTo("FAILED")
        result.output.contains(missingAsset.absolutePath).assertIsEqualTo(true, result.output)
        result.task(":githubReleaseUpload").assertIsEqualTo(null)
        invocations.exists().assertIsEqualTo(false)
    }
}
