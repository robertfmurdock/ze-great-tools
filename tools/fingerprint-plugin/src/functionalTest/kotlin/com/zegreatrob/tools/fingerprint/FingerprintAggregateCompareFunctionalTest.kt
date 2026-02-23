package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FingerprintAggregateCompareFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `compareAggregateFingerprints succeeds and prints bash-friendly match indicator when fingerprints are equal`() = setup(object {
        val expectedFilePath = "expected/aggregate-fingerprint.txt"
        val compareArgs = arrayOf("compareAggregateFingerprints", "--no-configuration-cache")
        val expectedFile = fileUnderProject(expectedFilePath)
    }) {
        writeSettings("compare-aggregate-fingerprints-success")

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            fingerprintConfig {
                compareToFile.set(layout.projectDirectory.file("$expectedFilePath"))
            }
            """.trimIndent(),
        )
    } exercise {
        val aggregateResult = gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))

        expectedFile.writeText(aggregateFingerprintFile(testProjectDir).readText())

        val compareResult = gradle(arguments = compareArgs)

        Pair(aggregateResult, compareResult)
    } verify { (aggregateResult, compareResult) ->
        assertEquals(TaskOutcome.SUCCESS, aggregateResult.task(":aggregateFingerprints")?.outcome)
        assertTrue(
            aggregateFingerprintFile(testProjectDir).exists(),
            "Aggregate fingerprint file should be generated at ${aggregateFingerprintFile(testProjectDir).path}",
        )

        assertEquals(TaskOutcome.SUCCESS, compareResult.task(":compareAggregateFingerprints")?.outcome)

        assertTrue(
            compareResult.output.lineSequence().any { it.trim() == "FINGERPRINT_MATCH=true" },
            "Expected a bash-friendly match indicator line `FINGERPRINT_MATCH=true` in output.\n--- output ---\n${compareResult.output}",
        )
    }

    @Test
    fun `compareAggregateFingerprints fails and prints bash-friendly mismatch indicator when fingerprints differ`() = setup(object {
        val expectedFilePath = "expected/aggregate-fingerprint.txt"
        val compareArgs = arrayOf("compareAggregateFingerprints", "--no-configuration-cache")
        val mismatchedFingerprint = "definitely-not-the-real-fingerprint"
        val expectedFile = fileUnderProject(expectedFilePath)
    }) {
        writeSettings("compare-aggregate-fingerprints-failure")

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            fingerprintConfig {
                compareToFile.set(layout.projectDirectory.file("$expectedFilePath"))
            }
            """.trimIndent(),
        )
    } exercise {
        val aggregateResult = gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))

        expectedFile.writeText(mismatchedFingerprint)

        val compareResult = gradle(
            arguments = compareArgs,
            expectFailure = true,
        )

        Pair(aggregateResult, compareResult)
    } verify { (aggregateResult, compareResult) ->
        assertEquals(TaskOutcome.SUCCESS, aggregateResult.task(":aggregateFingerprints")?.outcome)
        assertEquals(TaskOutcome.FAILED, compareResult.task(":compareAggregateFingerprints")?.outcome)

        assertTrue(
            compareResult.output.lineSequence().any { it.trim() == "FINGERPRINT_MATCH=false" },
            "Expected a bash-friendly mismatch indicator line `FINGERPRINT_MATCH=false` in output.\n--- output ---\n${compareResult.output}",
        )
    }

    @Test
    fun `compareAggregateFingerprints can be configured via -PfingerprintCompareToFile`() = setup(object {
        val expectedFilePath = "expected/aggregate-fingerprint.txt"
        val compareArgs = arrayOf(
            "compareAggregateFingerprints",
            "--no-configuration-cache",
            "-PfingerprintCompareToFile=expected/aggregate-fingerprint.txt",
        )
        val expectedFile = fileUnderProject(expectedFilePath)
    }) {
        writeSettings("compare-aggregate-fingerprints-property-config")

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )
    } exercise {
        val aggregateResult = gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))

        expectedFile.writeText(aggregateFingerprintFile(testProjectDir).readText())

        val compareResult = gradle(arguments = compareArgs)

        Pair(aggregateResult, compareResult)
    } verify { (aggregateResult, compareResult) ->
        assertEquals(TaskOutcome.SUCCESS, aggregateResult.task(":aggregateFingerprints")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, compareResult.task(":compareAggregateFingerprints")?.outcome)
        assertTrue(
            compareResult.output.lineSequence().any { it.trim() == "FINGERPRINT_MATCH=true" },
            "Expected `FINGERPRINT_MATCH=true` in output.\n--- output ---\n${compareResult.output}",
        )
    }
}
