package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

class GithubReleaseCreateCommandTest {
    @Test
    fun retriesReleaseCreationAfterRemoteTlsFailure() = setup(ReleaseCreationFixture()) {
        respondWith("view:release not found", "create:remote error: tls: internal error", "view:release not found", "create:")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(0)
        events().assertIsEqualTo(listOf("view", "create", "sleep", "view", "create"))
    }

    @Test
    fun existingReleaseSkipsCreation() = setup(ReleaseCreationFixture()) {
        respondWith("view:")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(0)
        events().assertIsEqualTo(listOf("view"))
    }

    @Test
    fun retryFindsReleaseCreatedBeforeResponseWasLost() = setup(ReleaseCreationFixture()) {
        respondWith("view:release not found", "create:published:connection reset by peer")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(0)
        events().assertIsEqualTo(listOf("view", "create", "sleep", "view"))
    }

    @Test
    fun retriesLookupBeforeCreatingRelease() = setup(ReleaseCreationFixture()) {
        respondWith("view:HTTP 502: Bad Gateway", "view:release not found", "create:")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(0)
        events().assertIsEqualTo(listOf("view", "sleep", "view", "create"))
    }

    @Test
    fun waitsAtLeastOneMinuteForSecondaryRateLimit() = setup(ReleaseCreationFixture()) {
        respondWith("view:HTTP 403: You have exceeded a secondary rate limit.", "view:")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(0)
        events().assertIsEqualTo(listOf("view", "sleep", "view"))
        delays().assertIsEqualTo(listOf(60.5))
    }

    @Test
    fun failsImmediatelyWhenLookupIsUnauthorized() = setup(ReleaseCreationFixture("HTTP 401: Bad credentials")) {
        respondWith("view:$diagnostic")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        events().assertIsEqualTo(listOf("view"))
    }

    @Test
    fun failsImmediatelyWhenCreationIsInvalid() = setup(ReleaseCreationFixture("HTTP 422: Validation Failed")) {
        respondWith("view:release not found", "create:$diagnostic")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        events().assertIsEqualTo(listOf("view", "create"))
    }

    @Test
    fun failsImmediatelyOnGenericForbiddenResponse() = setup(ReleaseCreationFixture("HTTP 403: Resource not accessible by integration")) {
        respondWith("view:$diagnostic")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        events().assertIsEqualTo(listOf("view"))
    }

    @Test
    fun failsImmediatelyWhenCertificateVerificationFails() = setup(
        ReleaseCreationFixture("tls: failed to verify certificate: x509: certificate signed by unknown authority"),
    ) {
        respondWith("view:$diagnostic")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        events().assertIsEqualTo(listOf("view"))
    }

    @Test
    fun reportsTheFinalFailureAfterFourAttempts() = setup(ReleaseCreationFixture("net/http: TLS handshake timeout")) {
        respondWith("view:HTTP 500", "view:HTTP 503", "view:HTTP 504", "view:$diagnostic")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        events().assertIsEqualTo(listOf("view", "sleep", "view", "sleep", "view", "sleep", "view"))
        delays().assertIsEqualTo(listOf(1.5, 2.5, 4.5))
    }

    @Test
    fun reconcilesReleaseAfterTheFinalCreateLosesItsResponse() = setup(ReleaseCreationFixture()) {
        respondWith(
            "view:HTTP 500",
            "view:HTTP 503",
            "view:HTTP 504",
            "view:release not found",
            "create:published:connection reset by peer",
        )
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(0)
        events().assertIsEqualTo(listOf("view", "sleep", "view", "sleep", "view", "sleep", "view", "create", "view"))
    }

    @Test
    fun doesNotMakeAnotherRequestAfterFinalCreateIsRateLimited() = setup(ReleaseCreationFixture("HTTP 403: You have exceeded a secondary rate limit.")) {
        respondWith("view:HTTP 500", "view:HTTP 503", "view:HTTP 504", "view:release not found", "create:$diagnostic")
    } exercise {
        runReleaseCommand()
    } verify { result ->
        result.exitCode.assertIsEqualTo(1)
        result.output.lineSequence().any { it == diagnostic }.assertIsEqualTo(true)
        events().assertIsEqualTo(listOf("view", "sleep", "view", "sleep", "view", "sleep", "view", "create"))
    }

    private data class CommandResult(val exitCode: Int, val output: String)

    private class ReleaseCreationFixture(val diagnostic: String = "") : AutoCloseable {
        private val projectDir = createTempDirectory().toFile()
        private val events = File(projectDir, "events")
        private val waits = File(projectDir, "waits")
        private val released = File(projectDir, "released")

        private val bin = File(projectDir, "bin").apply { mkdirs() }
        private val project by lazy { ProjectBuilder.builder().withProjectDir(projectDir).build() }

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

        override fun close() {
            projectDir.deleteRecursively()
        }
    }
}
