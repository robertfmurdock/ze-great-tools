package com.zegreatrob.tools.tagger.cli

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import java.io.File
import kotlin.test.Test

class ReadmeTest {
    private fun readReadme(): String {
        val readmePath = "command-line-tools/tagger-cli/README.md"
        val projectRoot = System.getProperty("user.dir")
        val readmeFile = File(projectRoot).parentFile.parentFile.resolve(readmePath)
        return if (readmeFile.exists()) {
            readmeFile.readText()
        } else {
            File(readmePath).readText()
        }
    }

    @Test
    fun readmeExistsAndIsReadable() = setup(object {
        val readmeContent = readReadme()
    }) exercise {
        readmeContent.isNotEmpty()
    } verify { result ->
        result.assertIsEqualTo(true)
    }

    @Test
    fun readmeReferencesMainHelp() = setup(object {
        val readmeContent = readReadme()
    }) exercise {
        readmeContent.contains("tagger --help")
    } verify { result ->
        result.assertIsEqualTo(true)
    }

    @Test
    fun readmeReferencesCalculateVersionHelp() = setup(object {
        val readmeContent = readReadme()
    }) exercise {
        readmeContent.contains("calculate-version --help")
    } verify { result ->
        result.assertIsEqualTo(true)
    }

    @Test
    fun readmeDoesNotDuplicateFieldDocumentation() = setup(object {
        val readmeContent = readReadme()
    }) exercise {
        val hasFieldDocPattern = Regex("""^\s*-\s+`data\.\w+`:\s+\w""", RegexOption.MULTILINE)
            .containsMatchIn(readmeContent)
        val hasFieldsHeader = readmeContent.contains("**Fields:**")
        hasFieldDocPattern || hasFieldsHeader
    } verify { result ->
        result.assertIsEqualTo(false)
    }

    @Test
    fun readmeDoesNotDuplicateErrorCodeDocumentation() = setup(object {
        val readmeContent = readReadme()
    }) exercise {
        val hasErrorCodesSection = readmeContent.contains("### Error Codes")
        val hasErrorCodePattern = Regex("""^\s*-\s+`[A-Z_]+_ERROR`:\s+\w""", RegexOption.MULTILINE)
            .containsMatchIn(readmeContent)
        hasErrorCodesSection || hasErrorCodePattern
    } verify { result ->
        result.assertIsEqualTo(false)
    }

    @Test
    fun readmeDoesNotDuplicateSnapshotReasonDocumentation() = setup(object {
        val readmeContent = readReadme()
    }) exercise {
        // Check for pattern like "- `DIRTY`: explanation text" or "- DIRTY - explanation"
        // but exclude the JSON example which shows enum values in an array
        val lines = readmeContent.lines()
        val hasSnapshotReasonDocs = lines.any { line ->
            val trimmed = line.trim()
            // Match documentation pattern but not JSON array content
            (
                trimmed.matches(Regex("""^-\s+`?(DIRTY|AHEAD|BEHIND|FORCED|NOT_RELEASE_BRANCH|NO_NEW_VERSION)`?[:\-]\s+\w.*""")) &&
                    !line.contains("\"DIRTY\"") && !line.contains("\"AHEAD\"")
                )
        }
        hasSnapshotReasonDocs
    } verify { result ->
        result.assertIsEqualTo(false)
    }
}
