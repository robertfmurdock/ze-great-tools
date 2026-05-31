package com.zegreatrob.tools.digger.cli

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import java.io.File
import kotlin.test.Test

class WhyDiggerTest {
    @Test
    fun whyDiggerDocumentExistsAndIsReadable() = setup(object {
    }) exercise {
        readWhyDigger()
    } verify { content ->
        content.isNotEmpty().assertIsEqualTo(true)
    }

    @Test
    fun whyDiggerContainsRequiredSections() = setup(object {
        val requiredSections = listOf(
            "Why Digger?",
            "Digger Principles",
            "Not For You If",
            "Scope Boundary",
            "Fast \"Should We Use It?\" Questions",
            "Important Tradeoffs",
            "Failure Modes",
        )
    }) exercise {
        readWhyDigger()
    } verify { content ->
        requiredSections.forEach { section ->
            content
                .contains(section)
                .assertIsEqualTo(true, "Expected why-digger.md to contain section: $section")
        }
    }
}

private fun readWhyDigger(): String {
    val whyDiggerPath = "docs/why-digger.md"
    val projectRoot = System.getProperty("user.dir")
    val whyDiggerFile = File(projectRoot).parentFile.parentFile.resolve(whyDiggerPath)
    return if (whyDiggerFile.exists()) {
        whyDiggerFile.readText()
    } else {
        File(whyDiggerPath).readText()
    }
}
