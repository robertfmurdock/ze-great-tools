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
        // Check for absence of detailed field documentation that duplicates CLI help
        readmeContent.contains("Boolean indicating if this is a snapshot version")
    } verify { result ->
        result.assertIsEqualTo(false)
    }
}
