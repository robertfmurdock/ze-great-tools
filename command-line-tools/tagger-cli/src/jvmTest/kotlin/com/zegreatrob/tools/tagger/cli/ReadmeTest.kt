package com.zegreatrob.tools.tagger.cli

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.tagger.core.SnapshotReason
import java.io.File
import kotlin.test.Test

class ReadmeTest {
    @Test
    fun readmeExistsAndIsReadable() = setup(object {
    }) exercise {
        readReadme()
    } verify { readme ->
        readme.isNotEmpty().assertIsEqualTo(true)
    }

    @Test
    fun readmeReferencesMainHelp() = setup(object {
    }) exercise {
        readReadme()
    } verify { readme ->
        readme.contains("tagger --help").assertIsEqualTo(true)
    }

    @Test
    fun readmeReferencesCalculateVersionHelp() = setup(object {
    }) exercise {
        readReadme()
    } verify { readme ->
        readme.contains("calculate-version --help").assertIsEqualTo(true)
    }

    @Test
    fun readmeDoesNotDuplicateFieldDocumentation() = setup(object {
    }) exercise {
        readReadme()
    } verify { readme ->
        containsFieldDocumentation(readme).assertIsEqualTo(false)
    }

    @Test
    fun readmeDoesNotDuplicateErrorCodeDocumentation() = setup(object {
    }) exercise {
        readReadme()
    } verify { readme ->
        containsErrorCodeDocumentation(readme).assertIsEqualTo(false)
    }

    @Test
    fun readmeDoesNotDuplicateSnapshotReasonDocumentation() = setup(object {
    }) exercise {
        readReadme()
    } verify { readme ->
        containsSnapshotReasonDocumentation(readme).assertIsEqualTo(false)
    }
}

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

private fun containsFieldDocumentation(content: String): Boolean {
    val fieldDocPattern = Regex("""^\s*-\s+`data\.\w+`:\s+\w""", RegexOption.MULTILINE)
    return fieldDocPattern.containsMatchIn(content) || content.contains("**Fields:**")
}

private fun containsErrorCodeDocumentation(content: String): Boolean {
    val errorCodePattern = Regex("""^\s*-\s+`[A-Z_]+_ERROR`:\s+\w""", RegexOption.MULTILINE)
    return content.contains("### Error Codes") || errorCodePattern.containsMatchIn(content)
}

private fun containsSnapshotReasonDocumentation(content: String): Boolean {
    val reasonNames = SnapshotReason.entries.joinToString("|") { it.name }
    val docPattern = Regex("""^\s*-\s+`?($reasonNames)`?[:\-]\s+\w""", RegexOption.MULTILINE)

    return content.lines().any { line ->
        val trimmed = line.trim()
        docPattern.matches(trimmed) && !trimmed.startsWith("\"") && !line.contains("\"DIRTY\"")
    }
}
