package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.runProcess
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.InitOp
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TaggerPluginCalculateVersionFunctionalTest : CalculateVersionTestSpec {
    @field:TempDir
    override lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }
    override val addFileNames: Set<String>
        get() = setOf(buildFile, settingsFile, ignoreFile).map { it.name }.toSet()

    @BeforeTest
    fun setup() {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
    }

    override fun setupWithDefaults() {
        buildFile.writeText(
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

    override fun setupWithOverrides(
        implicitPatch: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
    ) {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "master"
                ${if (implicitPatch != null) "implicitPatch.set($implicitPatch)" else ""}
                ${if (majorRegex != null) "majorRegex.set(Regex(\"${majorRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (minorRegex != null) "minorRegex.set(Regex(\"${minorRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (patchRegex != null) "patchRegex.set(Regex(\"${patchRegex.replace("\\", "\\\\")}\"))" else ""}
                ${if (noneRegex != null) "noneRegex.set(Regex(\"${noneRegex.replace("\\", "\\\\")}\"))" else ""}
                ${
                if (versionRegex != null) {
                    "versionRegex.set(Regex(\"${
                        versionRegex.replace(
                            "\\",
                            "\\\\",
                        )
                    }\"))"
                } else {
                    ""
                }
            }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun tagWillTagAndPushSuccessfully() {
        setupWithDefaults()

        val originDirectory = createTempDirectory()
        val originGrgit = Grgit.init(fun InitOp.() {
            this.dir = originDirectory.absolutePathString()
        })
        disableGpgSign(originDirectory.toFile())
        originGrgit.commit(fun CommitOp.() {
            this.message = "init"
        })
        val grgit = initializeGitRepo(
            listOf("init", "[patch] commit 1", "[patch] commit 2"),
            remoteUrl = originDirectory.absolutePathString(),
        )
        grgit.push()

        runProcess(listOf("git", "config", "user.email", "test@zegreatrob.com"), this.projectDir.absolutePath)
        runProcess(listOf("git", "config", "user.name", "RoB as Test"), this.projectDir.absolutePath)

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("tag", "-Pversion=1.0.0")
        runner.withProjectDir(projectDir)
        runner.build()

        val gitAdapter = GitAdapter(this.projectDir.absolutePath)
        assertEquals("1.0.0", gitAdapter.showTag("HEAD"))
    }

    @Test
    fun unifiedGroupWillReportErrorsWhenMissingGroupsWithCorrectNames() {
        setupWithOverrides(implicitPatch = true, versionRegex = ".*")

        initializeGitRepo(listOf("init", "commit (no) 1"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runCatching { runner.build() }.exceptionOrNull()

        assertContains(
            charSequence = result.toString(),
            other = "version regex must include groups named 'major', 'minor', 'patch', and 'none'.",
        )
    }

    override fun runCalculateVersion(): String {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()
        return result.output.trim()
    }

    private fun disableGpgSign(directory: File) {
        FileOutputStream(directory.resolve(".git/config"), true)
            .writer().use {
                it.write("[commit]\n        gpgsign = false")
            }
    }
}
