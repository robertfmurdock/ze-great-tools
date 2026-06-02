package com.zegreatrob.tools.tagger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.runProcess
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.createTempDirectory
import com.zegreatrob.tools.test.git.getEnvironmentVariable
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.removeDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

interface CalculateVersionTestSpec {
    var projectDir: String
    val addFileNames: Set<String>

    @BeforeTest
    fun setUpProjectDir() {
        projectDir = createTempDirectory()
    }

    @AfterTest
    fun deleteProjectDir() {
        removeDirectory(projectDir)
    }

    fun configureWithDefaults()

    fun configureWithOverrides(
        implicitPatch: Boolean? = null,
        allowDetachedHead: Boolean? = null,
        majorRegex: String? = null,
        minorRegex: String? = null,
        patchRegex: String? = null,
        versionRegex: String? = null,
        noneRegex: String? = null,
        forceSnapshot: Boolean? = null,
        warningsAsErrors: Boolean? = null,
    )

    fun initializeGitRepo(
        commits: List<String>,
        initialTag: String? = null,
        remoteUrl: String = projectDir,
    ) = initializeGitRepo(
        directory = projectDir,
        remoteUrl = remoteUrl,
        addFileNames = addFileNames,
        initialTag = initialTag,
        commits = commits,
    )

    @BeforeTest
    fun checkPrerequisites() {
        getEnvironmentVariable("GIT_CONFIG_GLOBAL").assertIsEqualTo(
            "/dev/null",
            "Ensure this is set for the test to work as intended",
        )
        getEnvironmentVariable("GIT_CONFIG_SYSTEM").assertIsEqualTo(
            "/dev/null",
            "Ensure this is set for the test to work as intended",
        )
    }

    fun execute(): TestResult

    fun warningFeatureToken(feature: String): String = feature

    fun TestResult.Success.assertHasDeprecationWarning(
        deprecatedFeature: String,
        replacement: String,
    ) {
        val deprecatedToken = warningFeatureToken(deprecatedFeature)
        val replacementToken = warningFeatureToken(replacement)
        warnings.any { it.contains(deprecatedToken) && it.contains("deprecated") }
            .assertIsEqualTo(
                true,
                "Expected deprecation warning for $deprecatedToken. Warnings: $warnings",
            )
        warnings.any { it.contains(replacementToken) }
            .assertIsEqualTo(
                true,
                "Expected migration guidance to $replacementToken. Warnings: $warnings",
            )
    }

    fun TestResult.Failure.assertHasDeprecationWarningEscalationError(
        deprecatedFeature: String,
        replacement: String,
    ) {
        val deprecatedToken = warningFeatureToken(deprecatedFeature)
        val replacementToken = warningFeatureToken(replacement)
        reason.contains("deprecated", ignoreCase = true).assertIsEqualTo(
            true,
            "Expected deprecation context in failure output. Output:\n$reason",
        )
        reason.contains(deprecatedToken).assertIsEqualTo(
            true,
            "Expected deprecated setting in failure output. Output:\n$reason",
        )
        reason.contains(replacementToken).assertIsEqualTo(
            true,
            "Expected migration guidance in failure output. Output:\n$reason",
        )
    }

    @Test
    fun withNoTagsProducesError() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"))
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(
                Regex("Inappropriate configuration: repository has no tags.\\s*\n\\s*If this is a new repository, use `tag` to set the initial version."),
            ).assertIsEqualTo(
                true,
                "Expected missing tags error. Output:\n$reason",
            )
        }
    }

    @Test
    fun whenNoRemoteProduceError() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = addFileNames,
            commits = listOf("init", "commit (no) 1"),
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains("⚠️")
                .assertIsEqualTo(true, "Expected warning symbol. Output:\n$reason")
            reason.contains("RISK:")
                .assertIsEqualTo(true, "Expected risk section. Output:\n$reason")
            reason.contains("production releases")
                .assertIsEqualTo(true, "Expected consequence explanation. Output:\n$reason")
        }
    }

    @Test
    fun whenNoRemoteButAllowDetachedHeadIsTrueDoNotError() = setup(object {
    }) {
        configureWithOverrides(allowDetachedHead = true)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = addFileNames,
            commits = listOf("init", "commit (no) 1"),
            initialTag = "1.2.3",
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4-SNAPSHOT")
    }

    @Test
    fun whenAllowDetachedHeadOnReleaseBranchEmitWarning() = setup(object {
    }) {
        configureWithOverrides(allowDetachedHead = true)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = addFileNames,
            commits = listOf("init", "commit (master) 1"),
            initialTag = "1.0.0",
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().run {
            warnings.size
                .assertIsEqualTo(1, "Expected one warning. Warnings: $warnings")
            warnings.first()
                .contains("release branch")
                .assertIsEqualTo(true, "Expected release branch warning. Warning: ${warnings.first()}")
        }
    }

    @Test
    fun whenCurrentCommitAlreadyHasTagWillUseTag() = setup(object {
        val gitAdapter = GitAdapter(
            projectDir,
            mapOf(
                "PATH" to (getEnvironmentVariable("PATH") ?: ""),
                "GIT_CONFIG_GLOBAL" to (getEnvironmentVariable("GIT_CONFIG_GLOBAL") ?: ""),
                "GIT_CONFIG_SYSTEM" to (getEnvironmentVariable("GIT_CONFIG_SYSTEM") ?: ""),
            ),
        )
    }) {
        configureWithDefaults()
        gitAdapter.init()
        gitAdapter.config("commit.gpgsign", "false")
        gitAdapter.add(".")
        gitAdapter.addCommitWithMessage("test commit")
        gitAdapter.newAnnotatedTag("1.0.23", "HEAD", "test", "test")
        val currentBranch = gitAdapter.status().head
        if (currentBranch == "main") {
            gitAdapter.checkout("main")
        } else {
            gitAdapter.checkout("main", newBranch = true)
        }
        gitAdapter.addRemote(name = "origin", url = projectDir)
        gitAdapter.fetch()
        gitAdapter.setBranchUpstream("origin/main", "main")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.0.23-SNAPSHOT")
    }

    @Test
    fun whenPreviousTagDoesNotHaveThreeNumbersWillError() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"), initialTag = "1.2")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(
                "Inappropriate configuration: the most recent tag did not have all three semver components.",
            ).assertIsEqualTo(
                true,
                "Expected malformed tag error. Output:\n$reason",
            )
        }
    }

    @Test
    fun withAllPatchCommitsOnlyIncrementsPatch() = setup(object {
    }) {
        configureWithDefaults()
        initializeGitRepo(listOf("init", "[patch] commit 1", "[patch] commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4")
    }

    @Test
    fun givenNoImplicitPatchCalculatingVersionWithUnlabeledCommitsDoesNotIncrement() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = false)
        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun givenInitialTagWithSuffixIgnoreSuffixAndFollowNormalRules() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = false)
        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3-SNAPSHOT")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithUnlabeledCommitsIncrementsPatch() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = true)
        initializeGitRepo(commits = listOf("init", "commit 1", "commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4")
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithNoneAndThenUnlabeledCommitsIncrementsPatch() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = true)
        initializeGitRepo(commits = listOf("init", "[none] commit 1", "commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4")
    }

    @Test
    fun givenImplicitPatchCalculatingVersionWithNoneCommitsDoesNotIncrementAndIsAlwaysSnapshot() = setup(object {
    }) {
        configureWithOverrides(implicitPatch = true)
        initializeGitRepo(commits = listOf("init", "[None] commit 1", "[none] commit 2"), initialTag = "1.2.3")
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun withOneMinorCommitsOnlyIncrementsMinor() = setup(object {
        val commits = listOf("init", "[patch] commit 1", "[minor] commit 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithDefaults()
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.3.0")
    }

    @Test
    fun canReplaceMajorRegex() = setup(object {
        val majorRegex = ".*(big).*"
        val commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = false, majorRegex = majorRegex)
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("2.0.0")
    }

    @Test
    fun unifiedGroupRegexCanReplaceMajorRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "[patch] commit 1", "commit (big) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = versionRegex,
        )
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("2.0.0")
    }

    @Test
    fun unifiedGroupCanReplaceMinorRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = versionRegex,
        )
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.3.0")
    }

    @Test
    fun canReplaceMinorRegex() = setup(object {
        val minorRegex = ".*(middle).*"
        val commits = listOf("init", "[patch] commit 1", "commit (middle) 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = false, minorRegex = minorRegex)
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.3.0")
    }

    @Test
    fun canReplacePatchRegex() = setup(object {
        val patchRegex = ".*(tiny).*"
        val commits = listOf("init", "commit 1", "commit (tiny) 2", "commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = false, patchRegex = patchRegex)
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4")
    }

    @Test
    fun unifiedGroupCanReplacePatchRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "commit 1", "commit (widdle) 2", "commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = false,
            versionRegex = versionRegex,
        )
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4")
    }

    @Test
    fun canReplaceNoneRegex() = setup(object {
        val noneRegex = ".*(no).*"
        val commits = listOf("init", "commit (no) 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = true,
            noneRegex = noneRegex,
        )
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun unifiedGroupCanReplaceNoneRegex() = setup(object {
        val versionRegex = "(?<major>.*big.*)?(?<minor>.*mid.*)?(?<patch>.*widdle.*)?(?<none>.*no.*)?"
        val commits = listOf("init", "commit (no) 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(
            implicitPatch = true,
            versionRegex = versionRegex,
        )
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.3-SNAPSHOT")
    }

    @Test
    fun calculatingVersionWithOneMajorCommitsOnlyIncrementsMajor() = setup(object {
        val commits = listOf("init", "[major] commit 1", "[minor] commit 2", "[patch] commit 3")
        val initialTag = "1.2.3"
    }) {
        configureWithDefaults()
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("2.0.0")
    }

    @Test
    fun versionTagsInCommitBodyAreIgnored() = setup(object {
        val commits = listOf(
            "init",
            "[patch] document versioning\n\nChanging stdout format is [major], improving stderr is [patch].",
        )
        val initialTag = "1.2.3"
    }) {
        configureWithDefaults()
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4")
    }

    @Test
    fun unifiedGroupWillReportErrorsWhenMissingGroupsWithCorrectNames() = setup(object {
        val versionRegex = ".*"
        val commits = listOf("init", "commit (no) 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(implicitPatch = true, versionRegex = versionRegex)
        initializeGitRepo(commits = commits, initialTag = initialTag)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains(
                "version regex must include groups named 'major', 'minor', 'patch', and 'none'.",
            ).assertIsEqualTo(
                true,
                "Expected version regex groups error. Output:\n$reason",
            )
        }
    }

    @Test
    fun forceSnapshotMakesReleaseVersionsBecomeSnapshots() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(forceSnapshot = true)
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().message.assertIsEqualTo("1.2.4-SNAPSHOT")
    }

    @Test
    fun forceSnapshotReportsForcedReason() = setup(object {
        val commits = listOf("init", "[patch] commit 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(forceSnapshot = true)
        initializeGitRepo(
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Success>().run {
            message.assertIsEqualTo("1.2.4-SNAPSHOT")
            details.contains("FORCED").assertIsEqualTo(
                true,
                "Expected snapshot reason output to include FORCED. Details:\n$details",
            )
        }
    }

    @Test
    fun warningsAsErrorsCausesNonZeroExitWhenDetachedHeadWarningPresent() = setup(object {
        val commits = listOf("init", "commit 1")
        val initialTag = "1.2.3"
    }) {
        configureWithOverrides(allowDetachedHead = true, warningsAsErrors = true)
        initializeGitRepo(
            directory = projectDir,
            remoteUrl = null,
            addFileNames = addFileNames,
            commits = commits,
            initialTag = initialTag,
        )
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains("Running with allowDetachedHead on release branch").assertIsEqualTo(
                true,
                "Expected detached HEAD warning in failure output. Output:\n$reason",
            )
        }
    }

    @Test
    fun withLightweightTagShowsActionableErrorMessage() = setup(object {
        val lightweightTagName = "1.0.0"
    }) {
        configureWithDefaults()
        initializeGitRepo(
            commits = listOf("initial commit"),
            initialTag = null,
        )
        runProcess(listOf("git", "tag", lightweightTagName), projectDir)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains("lightweight")
                .assertIsEqualTo(true, "Expected lightweight tag guidance. Output:\n$reason")
            reason.contains(lightweightTagName)
                .assertIsEqualTo(true, "Expected tag name in failure output. Output:\n$reason")
            reason.contains("git tag -d $lightweightTagName")
                .assertIsEqualTo(true, "Expected delete guidance. Output:\n$reason")
            reason.contains("git tag -a $lightweightTagName")
                .assertIsEqualTo(true, "Expected annotate guidance. Output:\n$reason")
            reason.contains("git push --force origin $lightweightTagName")
                .assertIsEqualTo(true, "Expected push guidance. Output:\n$reason")
        }
    }

    @Test
    fun withMultipleLightweightTagsShowsAllTagsInErrorMessage() = setup(object {
        val tag1 = "1.0.0"
        val tag2 = "1.1.0"
    }) {
        configureWithDefaults()
        initializeGitRepo(
            commits = listOf("initial commit"),
            initialTag = null,
        )
        runProcess(listOf("git", "tag", tag1), projectDir)
        runProcess(listOf("git", "tag", tag2), projectDir)
    } exercise {
        execute()
    } verify { result ->
        result.assertIsOfType<TestResult.Failure>().run {
            reason.contains("2 tags")
                .assertIsEqualTo(true, "Expected tag count in error output. Output:\n$reason")
            reason.contains(tag1)
                .assertIsEqualTo(true, "Expected first tag in error output. Output:\n$reason")
            reason.contains(tag2)
                .assertIsEqualTo(true, "Expected second tag in error output. Output:\n$reason")
            reason.contains("they are lightweight")
                .assertIsEqualTo(true, "Expected plural lightweight guidance. Output:\n$reason")
        }
    }
}
