package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.json.ContributionParser.parseContribution
import com.zegreatrob.tools.digger.model.Contribution
import com.zegreatrob.tools.test.git.addCommitWithMessage
import com.zegreatrob.tools.test.git.addTag
import com.zegreatrob.tools.test.git.defaultAuthors
import com.zegreatrob.tools.test.git.delayLongEnoughToAffectGitDate
import com.zegreatrob.tools.test.git.initializeGitRepo
import com.zegreatrob.tools.test.git.mergeInBranch
import com.zegreatrob.tools.test.git.switchToNewBranch
import kotlinx.datetime.toKotlinInstant
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Tag
import java.io.File
import java.lang.Thread.sleep
import kotlin.test.Test
import kotlin.test.assertEquals

interface CurrentContributionTestSpec : SetupWithOverrides {
    var projectDir: File
    val addFileNames: Set<String>

    fun setupWithDefaults()
    fun runCurrentContributionData(): String

    @Test
    fun `currentContributionData will show authors and co-authors case insensitive`() {
        setupWithDefaults()

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf(
                """here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        val output = runCurrentContributionData()
        assertEquals(
            listOf(
                "first@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun `when label is set will apply it to contribution`() {
        val label = "extraSpecialLabel"
        setupWithOverrides(label = label)

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf("here's a message"),
        )
        val output = runCurrentContributionData()
        assertEquals(
            label,
            parseContribution(output)?.label,
        )
    }

    @Test
    fun `when label is not set will use directory name`() {
        setupWithDefaults()

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf("here's a message"),
        )
        val output = runCurrentContributionData()
        assertEquals(
            projectDir.name,
            parseContribution(output)?.label,
        )
    }

    @Test
    fun `when included currentContributionData will show semver level`() {
        setupWithDefaults()

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf(
                """[patch] here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )

        val output = runCurrentContributionData()
        assertEquals("Patch", parseSemver(output))
        assertEquals(null, parseStoryId(output))
    }

    @Test
    fun `when currentContributionData includes multiple semvers uses largest`() {
        setupWithDefaults()

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf(
                """[major] here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
                """[minor] here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        val output = runCurrentContributionData()
        assertEquals("Major", parseSemver(output))
        assertEquals(null, parseStoryId(output))
    }

    @Test
    fun `currentContributionData will include authors from multiple commits after last tag`() {
        setupWithDefaults()

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
                """another
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
                """.trimMargin(),
                """yet another
                |
                |
                |Co-authored-by: 4th Guy <fourth@guy.edu>
                """.trimMargin(),
            ),
        )
        val output = runCurrentContributionData()

        assertEquals(
            listOf(
                "first@guy.edu",
                "fourth@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun `currentContributionData will include most recent tag range when head is tagged`() {
        setupWithDefaults()

        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        grgit.addTag("earlier")

        grgit.addCommitWithMessage(
            """another
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
            """.trimMargin(),
        )
        grgit.addCommitWithMessage(
            """yet another
                |
                |
                |Co-authored-by: 4th Guy <fourth@guy.edu>
            """.trimMargin(),
        )
        grgit.addTag("now")

        val output = runCurrentContributionData()

        assertEquals(
            listOf(
                "fourth@guy.edu",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun `when head is tagged currentContributionData will use include tag info`() {
        setupWithDefaults()

        val grgit = initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            listOf(
                """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        grgit.addTag("earlier")

        grgit.addCommitWithMessage(
            """another
                |
                |
                |Co-authored-by: Third Guy <third@guy.edu>
            """.trimMargin(),
        )
        grgit.addCommitWithMessage(
            """yet another
                |
                |
                |Co-authored-by: 4th Guy <fourth@guy.edu>
            """.trimMargin(),
        )
        sleep(1000)
        val nowTag = grgit.addTag("now")

        val output = runCurrentContributionData()

        val contribution = parseContribution(output)
        assertEquals(nowTag?.name, contribution?.tagName)
        assertEquals(nowTag?.dateTime?.toInstant()?.toKotlinInstant(), contribution?.tagDateTime)
    }

    @Test
    fun `currentContributionData will not include authors from commits before last tag`() {
        setupWithDefaults()

        val grgit =
            initializeGitRepo(
                projectDirectoryPath = projectDir.absolutePath,
                addFileNames = addFileNames,
                listOf(
                    """here's a message
                |
                |
                |Co-authored-by: First Guy <first@guy.edu>
                |Co-authored-by: Second Gui <second@gui.io>
                    """.trimMargin(),
                ),
            )

        grgit.addTag("release")
        grgit.addCommitWithMessage(
            """here's a message
                |
                |
                |Co-authored-by: Third Guy <Third@Guy.edu>
                |Co-authored-by: 4th Gui <fourth@gui.io>
            """.trimMargin(),
        )

        val output = runCurrentContributionData()

        assertEquals(
            listOf(
                "fourth@gui.io",
                "funk@test.io",
                "test@funk.edu",
                "third@guy.edu",
            ),
            parseCurrentAuthors(output),
        )
    }

    @Test
    fun canReplaceMajorRegex() {
        setupWithOverrides(majorRegex = ".*(big).*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (big) 2", "[patch] commit 3"),
        )

        val output = runCurrentContributionData()

        assertEquals("Major", parseContribution(output)?.semver)
    }

    @Test
    fun canReplaceMinorRegex() {
        setupWithOverrides(minorRegex = ".*mid.*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("[patch] commit 1", "commit (middle) 2", "[patch] commit 3"),
        )

        val output = runCurrentContributionData()

        assertEquals("Minor", parseContribution(output)?.semver)
    }

    @Test
    fun canReplacePatchRegex() {
        setupWithOverrides(patchRegex = ".*tiny.*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit 1", "commit (tiny) 2", "commit 3"),
        )
        val output = runCurrentContributionData()

        assertEquals("Patch", parseContribution(output)?.semver)
    }

    @Test
    fun canReplaceNoneRegex() {
        setupWithOverrides(noneRegex = ".*(no).*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit (no) 1"),
        )
        val output = runCurrentContributionData()

        assertEquals("None", parseContribution(output)?.semver)
    }

    @Test
    fun canReplaceStoryRegex() {
        setupWithOverrides(storyRegex = ".*-(?<storyId>.*-.*)-.*")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit -CowDog-99- 1"),
        )
        val output = runCurrentContributionData()

        val contribution = parseContribution(output)
        assertEquals("CowDog-99", contribution?.storyId)
    }

    @Test
    fun canReplaceEaseRegex() {
        setupWithOverrides(easeRegex = """.*\[(?<ease>[0-5])\].*""")

        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePath,
            addFileNames = addFileNames,
            commits = listOf("commit [4] 1"),
        )
        val output = runCurrentContributionData()

        val contribution = parseContribution(output)
        assertEquals(4, contribution?.ease)
    }

    @Test
    fun `will handle merge commits on merged branches correctly`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("first"))
        grgit.head()

        grgit.addTag("release")
        grgit.switchToNewBranch("branch2")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.switchToNewBranch("branch1")
        grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch2" }
        grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("fifth")

        grgit.mergeInBranch("branch2", "merge1")

        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("sixth")

        val merge2Commit = grgit.mergeInBranch("branch1", "merge2")
        delayLongEnoughToAffectGitDate()
        val secondRelease = grgit.addTag("release2")

        val allOutput = runCurrentContributionData()
        assertEquals(
            toContribution(
                lastCommit = merge2Commit,
                firstCommit = secondCommit,
                expectedCommitCount = 7,
                tag = secondRelease,
                expectedAuthors = defaultAuthors,
            ),
            parseContribution(allOutput),
        )
    }

    @Test
    fun `will ignore tags that do not match regex`() {
        setupWithOverrides(
            tagRegex = "v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?",
        )
        val grgit = initializeGitRepo(listOf("first"))
        grgit.head()

        grgit.addTag("v1.0.0")
        grgit.switchToNewBranch("branch2")
        val secondCommit = grgit.addCommitWithMessage("second")

        grgit.switchToNewBranch("branch1")
        grgit.addCommitWithMessage("third")

        grgit.checkout { it.branch = "branch2" }
        grgit.addCommitWithMessage("fourth")
        grgit.checkout { it.branch = "branch1" }
        grgit.addCommitWithMessage("fifth")

        grgit.mergeInBranch("branch2", "merge1")

        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("sixth")

        val merge2Commit = grgit.mergeInBranch("branch1", "merge2")
        delayLongEnoughToAffectGitDate()
        grgit.addTag("ignore-me")

        val allOutput = runCurrentContributionData()
        assertEquals(
            toContribution(
                lastCommit = merge2Commit,
                firstCommit = secondCommit,
                expectedCommitCount = 7,
                expectedAuthors = defaultAuthors,
            ),
            parseContribution(allOutput),
        )
    }

    @Test
    fun `will correctly understand longer running branch`() {
        setupWithDefaults()
        val grgit = initializeGitRepo(listOf("first"))
        grgit.head()

        grgit.addTag("release")
        grgit.switchToNewBranch("branch")
        val secondCommit = grgit.addCommitWithMessage("second")
        grgit.checkout { it.branch = "main" }
        grgit.addCommitWithMessage("third")
        delayLongEnoughToAffectGitDate()
        grgit.addTag("release2")
        grgit.checkout { it.branch = "branch" }
        grgit.addCommitWithMessage("fourth")
        grgit.addCommitWithMessage("fifth")
        grgit.checkout { it.branch = "main" }
        grgit.mergeInBranch("branch", "merge")
        val lastCommit = grgit.addCommitWithMessage("sixth")
        delayLongEnoughToAffectGitDate()
        val thirdRelease = grgit.addTag("release3")

        val allOutput = runCurrentContributionData()
        assertEquals(
            toContribution(
                lastCommit = lastCommit,
                firstCommit = secondCommit,
                expectedCommitCount = 5,
                tag = thirdRelease,
                expectedAuthors = defaultAuthors,
            ),
            parseContribution(allOutput),
        )
    }

    private fun toContribution(
        lastCommit: Commit,
        tag: Tag? = null,
        firstCommit: Commit = lastCommit,
        expectedAuthors: List<String>,
        expectedEase: Int? = null,
        expectedStoryId: String? = null,
        expectedCommitCount: Int = 1,
        expectedSemver: String? = null,
        expectedLabel: String = projectDir.name,
    ) = Contribution(
        lastCommit = lastCommit.id,
        dateTime = lastCommit.dateTime?.toInstant()?.toKotlinInstant(),
        firstCommit = firstCommit.id,
        firstCommitDateTime = firstCommit.dateTime?.toInstant()?.toKotlinInstant(),
        authors = expectedAuthors,
        label = expectedLabel,
        ease = expectedEase,
        storyId = expectedStoryId,
        tagName = tag?.name,
        tagDateTime = tag?.dateTime?.toInstant()?.toKotlinInstant(),
        commitCount = expectedCommitCount,
        semver = expectedSemver,
    )

    fun initializeGitRepo(commits: List<String>) = initializeGitRepo(
        projectDirectoryPath = projectDir.absolutePath,
        addFileNames = addFileNames,
        commits = commits,
    )
}
