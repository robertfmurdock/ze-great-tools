package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class GithubReleaseCreateCommandTest {
    @Test
    fun retriesReleaseCreationAfterRemoteTlsFailure() = setup(object {
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith("view:release not found", "create:remote error: tls: internal error", "view:release not found", "create:")
    } exercise {
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(0)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(0)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(0)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(0)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(0)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(1)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(1)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(1)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(1)
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
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        release.events().assertIsEqualTo(listOf("view", "sleep", "view", "sleep", "view", "sleep", "view"))
        release.delays().assertIsEqualTo(listOf(1.5, 2.5, 4.5))
    } teardown {
        release.close()
    }

    @Test
    fun reconcilesReleaseAfterTheFinalCreateLosesItsResponse() = setup(object {
        val release = ReleaseCreationFixture()
    }) {
        release.respondWith(
            "view:HTTP 500",
            "view:HTTP 503",
            "view:HTTP 504",
            "view:release not found",
            "create:published:connection reset by peer",
        )
    } exercise {
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(0)
        release.events().assertIsEqualTo(listOf("view", "sleep", "view", "sleep", "view", "sleep", "view", "create", "view"))
    } teardown {
        release.close()
    }

    @Test
    fun doesNotMakeAnotherRequestAfterFinalCreateIsRateLimited() = setup(object {
        val release = ReleaseCreationFixture()
        val diagnostic = "HTTP 403: You have exceeded a secondary rate limit."
    }) {
        release.respondWith("view:HTTP 500", "view:HTTP 503", "view:HTTP 504", "view:release not found", "create:$diagnostic")
    } exercise {
        release.runReleaseCommand()
    } verifyAnd { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        release.events().assertIsEqualTo(listOf("view", "sleep", "view", "sleep", "view", "sleep", "view", "create"))
    } teardown {
        release.close()
    }

    private data class CommandResult(val exitCode: Int, val output: String)

    private class ReleaseCreationFixture {
        private val projectDir = createTempDirectory().toFile()
        private val events = File(projectDir, "events")
        private val waits = File(projectDir, "waits")
        private val released = File(projectDir, "released")

        private val bin = File(projectDir, "bin").apply { mkdirs() }
        private val project = ProjectBuilder.builder().withProjectDir(projectDir).build()

        fun runReleaseCommand(): CommandResult {
            val arguments = project.objects.newInstance(GithubReleaseCreateArguments::class.java).apply {
                version.set("1.2.3")
                draft.set(false)
            }
            val process = ProcessBuilder(listOf("sh") + arguments.asArguments())
                .directory(projectDir)
                .redirectErrorStream(true)
                .apply { environment()["PATH"] = "${bin.absolutePath}:${System.getenv("PATH")}" }
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            return CommandResult(process.waitFor(), output)
        }

        fun respondWith(vararg responses: String) {
            writeGh(bin, responses)
            writeDelayCommands(bin)
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

        fun events() = events.readLines()

        fun delays() = waits.readLines().map(String::toDouble)

        fun close() {
            projectDir.deleteRecursively()
        }
    }
}
