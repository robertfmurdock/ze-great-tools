package com.zegreatrob.tools.digger.cli

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
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
