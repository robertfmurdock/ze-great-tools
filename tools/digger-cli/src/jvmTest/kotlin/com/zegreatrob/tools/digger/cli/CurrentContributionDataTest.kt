package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.testing.test
import com.zegreatrob.tools.digger.initializeGitRepo
import com.zegreatrob.tools.digger.parseCurrentAuthors
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentContributionDataTest {

    lateinit var projectDir: Path

    @BeforeTest
    fun setup() {
        projectDir = createTempDirectory()
    }

    @Test
    fun `currentContributionData will show authors and co-authors case insensitive`() {
        initializeGitRepo(
            projectDirectoryPath = projectDir.absolutePathString(),
            addFileNames = emptySet(),
            listOf(
                """here's a message
                |
                |
                |co-authored-by: First Guy <first@guy.edu>
                |CO-AUTHORED-BY: Second Gui <second@gui.io>
                """.trimMargin(),
            ),
        )
        val result = CurrentContributionData().test("--dir ${projectDir.absolutePathString()}")
        result.output
        assertEquals(
            listOf(
                "first@guy.edu",
                "funk@test.io",
                "second@gui.io",
                "test@funk.edu",
            ),
            parseCurrentAuthors(result.output),
        )
    }
}
