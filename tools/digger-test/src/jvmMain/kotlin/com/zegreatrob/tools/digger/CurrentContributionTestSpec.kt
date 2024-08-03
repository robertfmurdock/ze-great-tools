package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.json.ContributionParser.parseContribution
import kotlinx.datetime.toKotlinInstant
import java.io.File
import java.lang.Thread.sleep
import kotlin.test.Test
import kotlin.test.assertEquals

interface CurrentContributionTestSpec {
    var projectDir: File

    val addFileNames: Set<String>

    @Test
    fun `currentContributionData will show authors and co-authors case insensitive`() {
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
    fun `when included currentContributionData will show semver level`() {
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

    fun runCurrentContributionData(): String
}
