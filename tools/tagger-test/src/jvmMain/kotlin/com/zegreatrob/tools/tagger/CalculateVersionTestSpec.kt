package com.zegreatrob.tools.tagger

import com.zegreatrob.tools.test.git.disableGpgSign
import com.zegreatrob.tools.test.git.initializeGitRepo
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.AddOp
import org.ajoberstar.grgit.operation.CommitOp
import org.ajoberstar.grgit.operation.RemoteAddOp
import org.ajoberstar.grgit.operation.TagAddOp
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail

interface CalculateVersionTestSpec {
    var projectDir: File
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun configureWithOverrides(
        implicitPatch: Boolean? = null,
        majorRegex: String? = null,
        minorRegex: String? = null,
        patchRegex: String? = null,
        versionRegex: String? = null,
        noneRegex: String? = null,
    )

    fun initializeGitRepo(
        commits: List<String>,
        initialTag: String? = null,
        remoteUrl: String = projectDir.absolutePath,
    ) = initializeGitRepo(
        directory = projectDir.absolutePath,
        remoteUrl = remoteUrl,
        addFileNames = addFileNames,
        initialTag = initialTag,
        commits = commits,
    )

    fun execute(): TestResult
    fun runCalculateVersionSuccessfully(): String =
        when (val result = execute()) {
            is TestResult.Success -> result.message
            is TestResult.Failure -> fail("Expected success but got ${result.reason}")
        }

    @Test
    fun `calculating version with no tags produces zero version`() {
        setupWithDefaults()

        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"))
        val version = runCalculateVersionSuccessfully()

        assertEquals("0.0.0", version)
    }

    @Test
    fun `calculating version when current commit already has tag will use tag`() {
        setupWithDefaults()

        val grgit = Grgit.init(mapOf("dir" to projectDir.absolutePath))
        disableGpgSign(projectDir.absolutePath)
        grgit.add(
            fun(it: AddOp) {
                it.patterns = setOf(".")
            },
        )
        grgit.commit(
            fun(it: CommitOp) {
                it.message = "test commit"
            },
        )
        grgit.tag.add(
            fun(it: TagAddOp) {
                it.name = "1.0.23"
            },
        )
        grgit.checkout(mapOf<String?, Any>("branch" to "main", "createBranch" to true))
        grgit.remote.add(
            fun(it: RemoteAddOp) {
                it.name = "origin"
                it.url = projectDir.absolutePath
            },
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.0.23-SNAPSHOT", version)
    }

    @Test
    fun `calculating version with all patch commits only increments patch`() {
        setupWithDefaults()

        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun `given no implicit patch, calculating version with unlabeled commits does not increment`() {
        configureWithOverrides(implicitPatch = false)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `given initial tag with suffix, ignore suffix and follow normal rules`() {
        configureWithOverrides(implicitPatch = false)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3-SNAPSHOT")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `given implicit patch, calculating version with unlabeled commits increments patch`() {
        configureWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun `given implicit patch, calculating version with none and then unlabeled commits increments patch`() {
        configureWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "[none] commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun `given implicit patch, calculating version with None commits does not increment and is always snapshot`() {
        configureWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "[None] commit 1", "[none] commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `calculating version with one minor commits only increments minor`() {
        setupWithDefaults()

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "[minor] commit 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplaceMajorRegex() {
        configureWithOverrides(implicitPatch = false, majorRegex = ".*(big).*")

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupRegexCanReplaceMajorRegex() {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )

        val version = runCalculateVersionSuccessfully()

        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupCanReplaceMinorRegex() {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplaceMinorRegex() {
        configureWithOverrides(implicitPatch = false, minorRegex = ".*(middle).*")

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplacePatchRegex() {
        configureWithOverrides(implicitPatch = false, patchRegex = ".*(tiny).*")

        initializeGitRepo(commits = listOf("init", "commit 1", "commit (tiny) 2", "commit 3"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun unifiedGroupCanReplacePatchRegex() {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(commits = listOf("init", "commit 1", "commit (widdle) 2", "commit 3"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.4", version)
    }

    @Test
    fun canReplaceNoneRegex() {
        configureWithOverrides(
            implicitPatch = true,
            noneRegex = ".*(no).*",
        )

        initializeGitRepo(commits = listOf("init", "commit (no) 1"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun unifiedGroupCanReplaceNoneRegex() {
        configureWithOverrides(
            implicitPatch = true,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(commits = listOf("init", "commit (no) 1"), initialTag = "1.2.3")
        val version = runCalculateVersionSuccessfully()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `calculating version with one major commits only increments major`() {
        setupWithDefaults()

        initializeGitRepo(
            commits = listOf("init", "[major] commit 1", "[minor] commit 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersionSuccessfully()
        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupWillReportErrorsWhenMissingGroupsWithCorrectNames() {
        configureWithOverrides(implicitPatch = true, versionRegex = ".*")

        initializeGitRepo(listOf("init", "commit (no) 1"), "1.2.3")
        when (val result = execute()) {
            is TestResult.Failure -> assertContains(
                result.reason,
                "version regex must include groups named 'major', 'minor', 'patch', and 'none'.",
            )

            is TestResult.Success -> fail("Should not have succeeded.")
        }
    }
}