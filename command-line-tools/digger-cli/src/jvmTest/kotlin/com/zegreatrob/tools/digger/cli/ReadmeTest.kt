package com.zegreatrob.tools.digger.cli

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.digger.core.SemverType
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
        readme.contains("digger --help").assertIsEqualTo(true)
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
    fun readmeDoesNotDuplicateSemverTypeDocumentation() = setup(object {
    }) exercise {
        readReadme()
    } verify { readme ->
        containsSemverTypeDocumentation(readme).assertIsEqualTo(false)
    }
}

private fun readReadme(): String {
    val readmePath = "command-line-tools/digger-cli/README.md"
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

private fun containsSemverTypeDocumentation(content: String): Boolean {
    val typeNames = SemverType.entries.joinToString("|") { it.name }
    val docPattern = Regex("""^\s*-\s+`?($typeNames)`?[:\-]\s+\w""", RegexOption.MULTILINE)

    return content.lines().any { line ->
        val trimmed = line.trim()
        docPattern.matches(trimmed) && !trimmed.startsWith("\"") && !line.contains("\"$typeNames\"")
    }
}
