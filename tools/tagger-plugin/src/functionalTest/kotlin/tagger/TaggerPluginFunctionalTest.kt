package tagger

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.AddOp
import org.ajoberstar.grgit.operation.BranchChangeOp
import org.ajoberstar.grgit.operation.CheckoutOp
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.RemoteAddOp
import org.ajoberstar.grgit.operation.TagAddOp
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileOutputStream
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TaggerPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val ignoreFile by lazy { projectDir.resolve(".gitignore") }

    @BeforeTest
    fun setup() {
        settingsFile.writeText("")
        ignoreFile.writeText(".gradle")
    }

    @Test
    fun `calculating version with no tags produces zero version`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "main"
            }

            """.trimIndent(),
        )

        initializeGitRepo(listOf("[patch] commit 1", "[patch] commit 2"))
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("0.0.0", result.output.trim())
    }

    @Test
    fun `calculating version when current commit already has tag will use tag`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "main"
            }

            """.trimIndent(),
        )

        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        disableGpgSign()
        grgit.add(fun(it: AddOp) {
            it.patterns = setOf(".")
        })
        grgit.commit(fun(it: CommitOp) {
            it.message = "test commit"
        })
        grgit.tag.add(fun(it: TagAddOp) {
            it.name = "1.0.23"
        })
        grgit.checkout(mapOf<String?, Any>("branch" to "main", "createBranch" to true))
        grgit.remote.add(fun(it: RemoteAddOp) {
            it.name = "origin"
            it.url = projectDir.absolutePath
        })
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.0.23-SNAPSHOT", result.output.trim())
    }

    @Test
    fun `calculating version with all patch commits only increments patch`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "main"
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[patch] commit 1", "[patch] commit 2"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.4", result.output.trim())
    }

    @Test
    fun `given no implicit patch, calculating version with unlabeled commits does not increment`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "main"
                implicitPatch.set(false)
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("commit 1", "commit 2"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.3-SNAPSHOT", result.output.trim())
    }

    @Test
    fun `given implicit patch, calculating version with unlabeled commits increments patch`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "main"
                implicitPatch.set(true)
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("commit 1", "commit 2"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.4", result.output.trim())
    }

    @Test
    fun `given implicit patch, calculating version with none and then unlabeled commits increments patch`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "main"
                implicitPatch.set(true)
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[none] commit 1", "commit 2"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.4", result.output.trim())
    }

    @Test
    fun `given implicit patch, calculating version with None commits does not increment and is always snapshot`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            tagger {
                releaseBranch = "main"
                implicitPatch.set(true)
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[None] commit 1", "[none] commit 2"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.3-SNAPSHOT", result.output.trim())
    }

    @Test
    fun `calculating version with one minor commits only increments minor`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[patch] commit 1", "[minor] commit 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.3.0", result.output.trim())
    }

    @Test
    fun canReplaceMajorRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(false)
                majorRegex.set(Regex(".*(big).*"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("2.0.0", result.output.trim())
    }

    @Test
    fun unifiedGroupRegexCanReplaceMajorRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(false)
                versionRegex.set(Regex("(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("2.0.0", result.output.trim())
    }

    @Test
    fun canReplaceMinorRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(false)
                versionRegex.set(Regex("(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.3.0", result.output.trim())
    }

    @Test
    fun unifiedGroupCanReplaceMinorRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(false)
                minorRegex.set(Regex(".*(middle).*"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.3.0", result.output.trim())
    }

    @Test
    fun canReplacePatchRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(false)
                patchRegex.set(Regex(".*(tiny).*"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("commit 1", "commit (tiny) 2", "commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.4", result.output.trim())
    }

    @Test
    fun unifiedGroupCanReplacePatchRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(false)
                versionRegex.set(Regex("(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("commit 1", "commit (widdle) 2", "commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.4", result.output.trim())
    }

    @Test
    fun canReplaceNoneRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(true)
                noneRegex.set(Regex(".*(no).*"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("commit (no) 1"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.3-SNAPSHOT", result.output.trim())
    }

    @Test
    fun unifiedGroupCanReplaceNoneRegex() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(true)
                versionRegex.set(Regex("(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("commit (no) 1"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertEquals("1.2.3-SNAPSHOT", result.output.trim())
    }

    @Test
    fun unifiedGroupWillReportErrorsWhenMissingGroupsWithCorrectNames() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
                implicitPatch.set(true)
                versionRegex.set(Regex(".*"))
            }
            """.trimIndent(),
        )

        initializeGitRepo(listOf("commit (no) 1"), "1.2.3")
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

    @Test
    fun `calculating version with one major commits only increments major`() {
        buildFile.writeText(
            """
            plugins {
                id("com.zegreatrob.tools.tagger")
            }
            
            tagger {
                releaseBranch = "main"
            }

            """.trimIndent(),
        )

        initializeGitRepo(listOf("[major] commit 1", "[minor] commit 2", "[patch] commit 3"), "1.2.3")
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("calculateVersion", "-q")
        runner.withProjectDir(projectDir)
        val result = runner.build()
        assertEquals("2.0.0", result.output.trim())
    }

    private fun initializeGitRepo(
        additionalCommits: List<String> = listOf(),
        initialTag: String? = null,
    ) {
        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        disableGpgSign()
        grgit.add(fun AddOp.() {
            patterns = setOf(settingsFile.name, buildFile.name, ignoreFile.name)
        })

        grgit.commit(fun CommitOp.() {
            message = "test commit"
        })
        if (initialTag != null) {
            grgit.tag.add(fun(it: TagAddOp) {
                it.name = initialTag
            })
        }
        additionalCommits.forEach { message ->
            grgit.commit(fun(it: CommitOp) {
                it.message = message
            })
        }

        grgit.remote.add(fun RemoteAddOp.() {
            this.name = "origin"
            this.url = projectDir.absolutePath
        })
        grgit.checkout(fun CheckoutOp.() {
            branch = "main"
            createBranch = true
        })
        grgit.pull()
        grgit.branch.change(fun BranchChangeOp.() {
            this.name = "main"
            this.startPoint = "origin/main"
            this.mode = BranchChangeOp.Mode.TRACK
        })
    }

    private fun disableGpgSign() {
        FileOutputStream(projectDir.resolve(".git/config"), true)
            .writer().use {
                it.write("[commit]\n        gpgsign = false")
            }
    }
}
