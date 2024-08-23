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
import kotlin.test.assertEquals

interface CalculateVersionTestSpec {
    var projectDir: File
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun setupWithOverrides(
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

    fun runCalculateVersion(): String

    @Test
    fun `calculating version with no tags produces zero version`() {
        setupWithDefaults()

        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"))
        val version = runCalculateVersion()

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
        val version = runCalculateVersion()

        assertEquals("1.0.23-SNAPSHOT", version)
    }

    @Test
    fun `calculating version with all patch commits only increments patch`() {
        setupWithDefaults()

        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.4", version)
    }

    @Test
    fun `given no implicit patch, calculating version with unlabeled commits does not increment`() {
        setupWithOverrides(implicitPatch = false)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `given initial tag with suffix, ignore suffix and follow normal rules`() {
        setupWithOverrides(implicitPatch = false)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3-SNAPSHOT")
        val version = runCalculateVersion()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `given implicit patch, calculating version with unlabeled commits increments patch`() {
        setupWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.4", version)
    }

    @Test
    fun `given implicit patch, calculating version with none and then unlabeled commits increments patch`() {
        setupWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "[none] commit 1", "commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.4", version)
    }

    @Test
    fun `given implicit patch, calculating version with None commits does not increment and is always snapshot`() {
        setupWithOverrides(implicitPatch = true)

        initializeGitRepo(commits = listOf("init", "[None] commit 1", "[none] commit 2"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `calculating version with one minor commits only increments minor`() {
        setupWithDefaults()

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "[minor] commit 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersion()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplaceMajorRegex() {
        setupWithOverrides(implicitPatch = false, majorRegex = ".*(big).*")

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersion()

        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupRegexCanReplaceMajorRegex() {
        setupWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )

        val version = runCalculateVersion()

        assertEquals("2.0.0", version)
    }

    @Test
    fun unifiedGroupCanReplaceMinorRegex() {
        setupWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersion()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplaceMinorRegex() {
        setupWithOverrides(implicitPatch = false, minorRegex = ".*(middle).*")

        initializeGitRepo(
            commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersion()

        assertEquals("1.3.0", version)
    }

    @Test
    fun canReplacePatchRegex() {
        setupWithOverrides(implicitPatch = false, patchRegex = ".*(tiny).*")

        initializeGitRepo(commits = listOf("init", "commit 1", "commit (tiny) 2", "commit 3"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.4", version)
    }

    @Test
    fun unifiedGroupCanReplacePatchRegex() {
        setupWithOverrides(
            implicitPatch = false,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(commits = listOf("init", "commit 1", "commit (widdle) 2", "commit 3"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.4", version)
    }

    @Test
    fun canReplaceNoneRegex() {
        setupWithOverrides(
            implicitPatch = true,
            noneRegex = ".*(no).*",
        )

        initializeGitRepo(commits = listOf("init", "commit (no) 1"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun unifiedGroupCanReplaceNoneRegex() {
        setupWithOverrides(
            implicitPatch = true,
            versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?",
        )

        initializeGitRepo(commits = listOf("init", "commit (no) 1"), initialTag = "1.2.3")
        val version = runCalculateVersion()

        assertEquals("1.2.3-SNAPSHOT", version)
    }

    @Test
    fun `calculating version with one major commits only increments major`() {
        setupWithDefaults()

        initializeGitRepo(
            commits = listOf("init", "[major] commit 1", "[minor] commit 2", "[patch] commit 3"),
            initialTag = "1.2.3",
        )
        val version = runCalculateVersion()
        assertEquals("2.0.0", version)
    }
}
