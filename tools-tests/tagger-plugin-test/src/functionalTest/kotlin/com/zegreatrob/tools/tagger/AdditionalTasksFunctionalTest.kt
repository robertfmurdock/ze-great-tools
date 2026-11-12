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
    private data class PublicationRetryRun(
        val firstAttempt: BuildResult,
        val retry: BuildResult,
    )

    @Test
    fun retriesReleaseCreationAfterRemoteTlsFailure() = setup(object {
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:release not found", "create:remote error: tls: internal error", "view:release not found", "create:")
    } exercise {
        release.runner.build()
    } verifyAnd { result ->
        result.task(":githubRelease")?.outcome?.name.assertIsEqualTo("SUCCESS")
        release.events().assertIsEqualTo(listOf("view", "create", "sleep", "view", "create"))
    } teardown {
        release.close()
    }

    @Test
    fun existingReleaseSkipsCreation() = setup(object {
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:")
    } exercise {
        release.runner.build()
    } verifyAnd { result ->
        result.task(":githubRelease")?.outcome?.name.assertIsEqualTo("SUCCESS")
        release.events().assertIsEqualTo(listOf("view"))
    } teardown {
        release.close()
    }

    @Test
    fun retryFindsReleaseCreatedBeforeResponseWasLost() = setup(object {
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:release not found", "create:published:connection reset by peer")
    } exercise {
        release.runner.build()
    } verifyAnd { result ->
        result.task(":githubRelease")?.outcome?.name.assertIsEqualTo("SUCCESS")
        release.events().assertIsEqualTo(listOf("view", "create", "sleep", "view"))
    } teardown {
        release.close()
    }

    @Test
    fun retriesLookupBeforeCreatingRelease() = setup(object {
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:HTTP 502: Bad Gateway", "view:release not found", "create:")
    } exercise {
        release.runner.build()
    } verifyAnd { result ->
        result.task(":githubRelease")?.outcome?.name.assertIsEqualTo("SUCCESS")
        release.events().assertIsEqualTo(listOf("view", "sleep", "view", "create"))
    } teardown {
        release.close()
    }

    @Test
    fun waitsAtLeastOneMinuteForSecondaryRateLimit() = setup(object {
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:HTTP 403: You have exceeded a secondary rate limit.", "view:")
    } exercise {
        release.runner.build()
    } verifyAnd { result ->
        result.task(":githubRelease")?.outcome?.name.assertIsEqualTo("SUCCESS")
        release.events().assertIsEqualTo(listOf("view", "sleep", "view"))
        release.delays().assertIsEqualTo(listOf(60.5))
    } teardown {
        release.close()
    }

    @Test
    fun failsImmediatelyWhenLookupIsUnauthorized() = setup(object {
        val diagnostic = "HTTP 401: Bad credentials"
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:$diagnostic")
    } exercise {
        release.runner.buildAndFail()
    } verifyAnd { result ->
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        release.events().assertIsEqualTo(listOf("view"))
    } teardown {
        release.close()
    }

    @Test
    fun failsImmediatelyWhenCreationIsInvalid() = setup(object {
        val diagnostic = "HTTP 422: Validation Failed"
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:release not found", "create:$diagnostic")
    } exercise {
        release.runner.buildAndFail()
    } verifyAnd { result ->
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        release.events().assertIsEqualTo(listOf("view", "create"))
    } teardown {
        release.close()
    }

    @Test
    fun failsImmediatelyOnGenericForbiddenResponse() = setup(object {
        val diagnostic = "HTTP 403: Resource not accessible by integration"
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:$diagnostic")
    } exercise {
        release.runner.buildAndFail()
    } verifyAnd { result ->
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        release.events().assertIsEqualTo(listOf("view"))
    } teardown {
        release.close()
    }

    @Test
    fun failsImmediatelyWhenCertificateVerificationFails() = setup(object {
        val diagnostic = "tls: failed to verify certificate: x509: certificate signed by unknown authority"
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:$diagnostic")
    } exercise {
        release.runner.buildAndFail()
    } verifyAnd { result ->
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        release.events().assertIsEqualTo(listOf("view"))
    } teardown {
        release.close()
    }

    @Test
    fun reportsTheFinalFailureAfterFourAttempts() = setup(object {
        val diagnostic = "net/http: TLS handshake timeout"
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:HTTP 500", "view:HTTP 503", "view:HTTP 504", "view:$diagnostic")
    } exercise {
        release.runner.buildAndFail()
    } verifyAnd { result ->
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        release.events().assertIsEqualTo(listOf("view", "sleep", "view", "sleep", "view", "sleep", "view"))
        release.delays().assertIsEqualTo(listOf(1.5, 2.5, 4.5))
    } teardown {
        release.close()
    }

    private inner class ReleaseCreationFixture {
        private val projectDir = createTempDirectory()
        private val events = File(projectDir, "events")
        private val waits = File(projectDir, "waits")
        private val released = File(projectDir, "released")

        val runner: GradleRunner = GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("githubRelease", "-x", "tag", "-Pversion=1.2.3", "--console=plain")

        fun respondWith(vararg responses: String) {
            writeSettings(projectDir)
            val bin = File(projectDir, "bin").apply { mkdirs() }
            writeGh(bin, responses)
            writeDelayCommands(bin)
            writeReleaseBuild(bin)
        }

        private fun writeGh(bin: File, responses: Array<out String>) {
            val replies = File(projectDir, "replies").apply { writeText(responses.joinToString("\n", postfix = "\n")) }
            File(bin, "gh").apply {
                writeText(
                    """
                    #!/bin/sh
                    echo "$2" >> "${events.absolutePath}"
                    [ "$2" != view ] || [ ! -f "${released.absolutePath}" ] || exit 0
                    response=$(head -n 1 "${replies.absolutePath}")
                    sed '1d' "${replies.absolutePath}" > "${replies.absolutePath}.next"
                    mv "${replies.absolutePath}.next" "${replies.absolutePath}"
                    [ "${'$'}{response%%:*}" = "$2" ] || exit 2
                    diagnostic="${'$'}{response#*:}"
                    case "${'$'}diagnostic" in
                        "") exit 0 ;;
                        published:*) touch "${released.absolutePath}"; diagnostic="${'$'}{diagnostic#published:}" ;;
                    esac
                    printf '%s\n' "${'$'}diagnostic" >&2
                    exit 1
                    """.trimIndent(),
                )
                setExecutable(true)
            }
        }

        private fun writeDelayCommands(bin: File) {
            File(bin, "sleep").apply {
                writeText("#!/bin/sh\necho sleep >> '${events.absolutePath}'\necho ${'$'}1 >> '${waits.absolutePath}'\n")
                setExecutable(true)
            }
            File(bin, "od").apply {
                writeText("#!/bin/sh\necho ' 128'\n")
                setExecutable(true)
            }
        }

        private fun writeReleaseBuild(bin: File) {
            File(projectDir, "build.gradle.kts").writeText(
                """
                plugins { id("com.zegreatrob.tools.tagger") }
                tagger { githubReleaseEnabled.set(true) }
                tasks.named<Exec>("githubRelease") {
                    environment("PATH", "${bin.absolutePath}:" + System.getenv("PATH"))
                }
                """.trimIndent(),
            )
        }

        fun events() = events.readLines()

        fun delays() = waits.readLines().map(String::toDouble)

        fun close() = removeDirectory(projectDir)
    }

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
    ) = setup(object {
        val projectDir = createTempDirectory()
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
    } verifyAnd { result ->
        result.task(":generateDraftSetting")?.outcome?.name.assertIsEqualTo("SUCCESS")
        result.task(":githubReleasePublish")?.outcome?.name.assertIsEqualTo(expectedOutcome)
        (output.takeIf { it.exists() }?.readLines() ?: emptyList()).assertIsEqualTo(expectedArguments)
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun generatedDraftSettingControlsReleaseCreation() = setup(object {
        val projectDir = createTempDirectory()
        val output = File(projectDir, "gh-create-arguments.txt")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                if [ "$2" = "view" ]; then echo "release not found" >&2; exit 1; fi
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
        first to runner.build()
    } verifyAnd { (first, second) ->
        first.task(":generateDraftSetting")?.outcome?.name.assertIsEqualTo("SUCCESS")
        second.output.contains("Reusing configuration cache.").assertIsEqualTo(true)
        output.readLines().assertIsEqualTo(listOf("release", "create", "1.2.3", "--verify-tag", "--draft", "--title", "1.2.3", "--notes", "1.2.3"))
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun uploadSkipsWhenNoReleaseAssetsExist() = setup(object {
        val projectDir = createTempDirectory()
    }) {
        writeSettings(projectDir)
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.tagger") }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
            }
            """.trimIndent(),
        )
    } exercise {
        GradleRunner.create()
            .withProjectDir(File(projectDir))
            .withArguments("githubReleaseUpload", "-x", "githubRelease", "-Pversion=1.2.3", "--console=plain")
            .build()
    } verifyAnd { result ->
        result.task(":githubReleaseUpload")?.outcome?.name.assertIsEqualTo("SKIPPED")
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun generatedReleaseAssetIsUploaded() = setup(object {
        val projectDir = createTempDirectory()
        val output = File(projectDir, "gh-arguments.txt")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                if [ "$2" = "view" ]; then exit 1; fi
                printf '%s\n' "$@" > "${output.absolutePath}"
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
        first to runner.build()
    } verifyAnd { (first, second) ->
        first.task(":generateReleaseAsset")?.outcome?.name.assertIsEqualTo("SUCCESS")
        first.task(":githubReleaseUpload")?.outcome?.name.assertIsEqualTo("SUCCESS")
        second.output.contains("Reusing configuration cache.").assertIsEqualTo(true)
        output.readLines().assertIsEqualTo(listOf("release", "upload", "1.2.3", "build/assets/generated asset.zip"))
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun githubReleaseWaitsForDistributionsAndRunsAfterPublicationRetry() = setup(object {
        val projectDir = createTempDirectory()
        val events = File(projectDir, "events.txt")
    }) {
        writeSettings(projectDir)
        val bin = File(projectDir, "bin").apply { mkdirs() }
        File(bin, "gh").apply {
            writeText(
                """
                #!/bin/sh
                if [ "$2" = "view" ]; then echo "release not found" >&2; exit 1; fi
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
    } verifyAnd { (firstAttempt, retry) ->
        firstAttempt.output.contains("simulated Maven publication failure")
            .assertIsEqualTo(true, "Expected the first attempt to fail at the simulated Maven publication")
        retry.task(":githubRelease")?.outcome?.name.assertIsEqualTo("SUCCESS")
        events.readLines().assertIsEqualTo(listOf("maven-failure", "maven", "npm", "portal", "github"))
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun previousVersionOutputsLastTag() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
        val initialTag = "1.2.3"
    }) {
        writeSettings(projectDir)
        writeBuildFile(projectDir)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = initialTag,
            commits = listOf("init"),
        )
    } exercise {
        runGradle(projectDir, "previousVersion")
    } verifyAnd { output ->
        output.trim().assertIsEqualTo(initialTag)
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun commitReportOutputsReport() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
    }) {
        writeSettings(projectDir)
        writeBuildFile(projectDir)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init", "[patch] commit 1"),
        )
    } exercise {
        runGradle(projectDir, "commitReport")
    } verifyAnd { output ->
        output.contains("COMMIT REPORT-------").assertIsEqualTo(true)
        output.contains("COMMIT REPORT OVAH--").assertIsEqualTo(true)
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun releaseDryRunAvoidsImplicitGitHeadDependency() = setup(object {
        val projectDir = createTempDirectory()
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
        runGradle(
            projectDir = projectDir,
            task = "release",
            quiet = false,
            extraArgs = listOf("--dry-run", "--warning-mode=all", "--console=plain"),
        )
    } verifyAnd { output ->
        output.contains("implicit dependency")
            .assertIsEqualTo(false, "Unexpected implicit dependency warning.\n$output")
        output.contains("validation_problems")
            .assertIsEqualTo(false, "Unexpected validation problems output.\n$output")
    } teardown {
        removeDirectory(projectDir)
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
    fun githubReleaseUploadFailsWhenAssetFileMissing() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
        val missingFile = "does-not-exist.txt"
    }) {
        writeSettings(projectDir)
        writeBuildFileWithMissingAssetAndValidation(projectDir, missingFile)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init"),
        )
    } exercise {
        val runner = GradleRunner.create()
        runner.withProjectDir(File(projectDir))
        runner.withArguments("validateGithubReleaseAssets", "--console=plain")
        runCatching { runner.build() }
    } verifyAnd { result ->
        val exception = result.exceptionOrNull()
        result.isFailure
            .assertIsEqualTo(true, "Expected validation to fail when asset file missing")
        exception
            ?.message
            ?.contains(missingFile)
            .assertIsEqualTo(true, "Error message should mention missing file: ${exception?.message}")
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun githubReleaseUploadReportsAllMissingAssets() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
        val missingFiles = listOf("missing-one.txt", "missing-two.txt", "missing-three.txt")
    }) {
        writeSettings(projectDir)
        writeBuildFileWithMultipleMissingAssetsAndValidation(projectDir, missingFiles)
        writeGitIgnore(projectDir)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init"),
        )
    } exercise {
        val runner = GradleRunner.create()
        runner.withProjectDir(File(projectDir))
        runner.withArguments("validateGithubReleaseAssets", "--console=plain")
        runCatching { runner.build() }
    } verifyAnd { result ->
        val exception = result.exceptionOrNull()
        result.isFailure
            .assertIsEqualTo(true, "Expected validation to fail when asset files missing")
        val errorMessage = exception?.message ?: ""
        missingFiles.forEach { missingFile ->
            errorMessage
                .contains(missingFile)
                .assertIsEqualTo(true, "Error message should mention missing file '$missingFile': $errorMessage")
        }
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun validateGithubReleaseAssetsSucceedsWhenAllFilesExist() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
        val assetFiles = listOf("asset-one.txt", "asset-two.txt")
    }) {
        writeSettings(projectDir)
        writeBuildFileWithExistingAssets(projectDir, assetFiles)
        writeGitIgnore(projectDir)
        assetFiles.forEach { fileName ->
            File("$projectDir/$fileName").writeText("test content")
        }
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init"),
        )
    } exercise {
        val runner = GradleRunner.create()
        runner.withProjectDir(File(projectDir))
        runner.withArguments("validateGithubReleaseAssets", "--console=plain")
        runCatching { runner.build() }
    } verifyAnd { result ->
        result.isSuccess
            .assertIsEqualTo(true, "Expected validation to succeed when all files exist: ${result.exceptionOrNull()?.message}")
    } teardown {
        removeDirectory(projectDir)
    }

    @Test
    fun validateGithubReleaseAssetsSucceedsWhenFileTreeMatchesNoFiles() = setup(object {
        val projectDir = createTempDirectory()
        val addFileNames = setOf("build.gradle.kts", "settings.gradle", ".gitignore")
        val buildDir = "$projectDir/build"
        val distDir = "$buildDir/distributions"
    }) {
        writeSettings(projectDir)
        writeBuildFileWithFileTreeAssets(projectDir)
        writeGitIgnore(projectDir)
        File(distDir).mkdirs()
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = projectDir,
            addFileNames = addFileNames,
            initialTag = "1.0.0",
            commits = listOf("init"),
        )
    } exercise {
        val runner = GradleRunner.create()
        runner.withProjectDir(File(projectDir))
        runner.withArguments("validateGithubReleaseAssets", "--console=plain")
        val result = runner.build()
        result.output
    } verifyAnd { output ->
        output
            .contains("No GitHub release assets configured")
            .assertIsEqualTo(true, "Should report no assets when fileTree matches nothing: $output")
    } teardown {
        removeDirectory(projectDir)
    }

    private fun writeBuildFileWithMissingAssetAndValidation(projectDir: String, missingFile: String) {
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseAssets.from(file("$missingFile"))
            }
            """.trimIndent(),
        )
    }

    private fun writeBuildFileWithMultipleMissingAssetsAndValidation(projectDir: String, missingFiles: List<String>) {
        val filesDeclaration = missingFiles.joinToString(", ") { "file(\"$it\")" }
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseAssets.from($filesDeclaration)
            }
            """.trimIndent(),
        )
    }

    private fun writeBuildFileWithExistingAssets(projectDir: String, assetFiles: List<String>) {
        val filesDeclaration = assetFiles.joinToString(", ") { "file(\"$it\")" }
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseAssets.from($filesDeclaration)
            }
            """.trimIndent(),
        )
    }

    private fun writeBuildFileWithFileTreeAssets(projectDir: String) {
        File("$projectDir/build.gradle.kts").writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                githubReleaseEnabled.set(true)
                githubReleaseAssets.from(
                    fileTree("build/distributions") {
                        include("existing.zip", "missing.zip")
                    }
                )
            }
            """.trimIndent(),
        )
    }

    private fun runGradle(
        projectDir: String,
        task: String,
        quiet: Boolean = true,
        extraArgs: List<String> = emptyList(),
    ): String {
        val runner = GradleRunner.create()
        runner.withProjectDir(File(projectDir))
        val args = mutableListOf(task)
        if (quiet) {
            args.add("-q")
        }
        args.addAll(extraArgs)
        runner.withArguments(args)
        return runner.build().output.trim()
    }
}
