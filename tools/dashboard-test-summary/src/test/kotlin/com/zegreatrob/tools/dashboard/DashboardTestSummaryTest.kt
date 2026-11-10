package com.zegreatrob.tools.dashboard

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import kotlinx.serialization.json.Json
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class DashboardTestSummaryTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `writes workflow identity and aggregated testcase outcomes`() = setup(object {
        val expected = Json.parseToJsonElement(
            """{"schemaVersion":1,"runId":42,"runAttempt":2,"headSha":"abc","total":5,"passed":1,"failed":2,"skipped":2}""",
        )
        val build = fixture(
            """
            dashboardTestSummary.reports.from(files("first.xml", "second.xml"))
            """.trimIndent(),
        )
    }) exercise {
        runCatching { build.build() }
    } verify { result ->
        result.getOrThrow()
        summary().assertIsEqualTo(expected)
    }

    @Test
    fun `counts a shared included build once and reuses configuration cache`() = setup(object {
        val build = composite()
        val expected = Json.parseToJsonElement(
            """{"schemaVersion":1,"runId":42,"runAttempt":2,"headSha":"abc","total":5,"passed":5,"failed":0,"skipped":0}""",
        )
    }) exercise {
        runCatching {
            build.build()
            build.build()
        }
    } verify { result ->
        result.getOrThrow().output.contains("Reusing configuration cache.").assertIsEqualTo(true)
        summary().assertIsEqualTo(expected)
    }

    @Test
    fun `a failing included test contributes its XML while the build fails`() = setup(object {
        val build = compositeWithProducer(testBody = "org.junit.Assert.fail(\"expected assertion failure\");")
        val expected = Json.parseToJsonElement(
            """{"schemaVersion":1,"runId":42,"runAttempt":2,"headSha":"abc","total":2,"passed":1,"failed":1,"skipped":0}""",
        )
    }) exercise {
        runCatching { build.buildAndFail() }
    } verify { result ->
        result.getOrThrow().output.contains(":producer:test FAILED").assertIsEqualTo(true)
        summary().assertIsEqualTo(expected)
    }

    @Test
    fun `a producer crash leaves no summary from an earlier attempt`() = setup(object {
        val build = compositeWithProducer(testConfiguration = """doFirst { error("runner crashed before writing XML") }""")
        val previousSummary = directory.resolve("build/dashboard/test-summary.json").apply {
            parentFile.mkdirs()
            writeText("""{"schemaVersion":1,"runId":42,"runAttempt":1,"headSha":"abc","total":9}""")
        }
        val previousXml = directory.resolve("producer/build/custom-junit/TEST-old.xml").apply {
            parentFile.mkdirs()
            writeText("""<testsuite><testcase name="old"/></testsuite>""")
        }
    }) exercise {
        runCatching { build.buildAndFail() }
    } verify { result ->
        result.getOrThrow().output.contains("runner crashed before writing XML").assertIsEqualTo(true)
        previousSummary.exists().assertIsEqualTo(false)
    }

    private fun compositeWithProducer(
        testBody: String = "",
        testConfiguration: String = "",
    ): GradleRunner {
        settings(directory, "fixture", """dashboardTestSummary.includeBuild("producer", "example:producer")""")
        report(directory)
        val producer = directory.resolve("producer")
        settings(producer, "producer")
        javaTest(producer, testBody, testConfiguration)
        return runner()
    }

    private fun composite(): GradleRunner {
        settings(
            directory,
            "fixture",
            """
            include("sub")
            dashboardTestSummary {
                includeBuild("left", "example:left")
                includeBuild("right", "example:right")
                includeBuild("empty", "example:empty")
            }
            """.trimIndent(),
        )
        report(directory)
        javaTest(directory.resolve("sub"))
        for (name in listOf("left", "right")) {
            val child = directory.resolve(name)
            settings(child, name, """dashboardTestSummary.includeBuild("../shared", "example:shared")""")
            report(child)
        }
        val shared = directory.resolve("shared")
        settings(shared, "shared")
        report(shared)
        val empty = directory.resolve("empty")
        settings(empty, "empty")
        empty.resolve("build.gradle.kts").writeText("""group = "example"""".trimIndent())
        return runner()
    }

    private fun report(project: File) {
        project.resolve("build.gradle.kts").writeText(
            """
            group = "example"
            dashboardTestSummary.reports.from(files("report.xml"))
            """.trimIndent(),
        )
        project.resolve("report.xml").writeText("""<testsuite><testcase name="passes"/></testsuite>""")
    }

    private fun javaTest(project: File, testBody: String = "", testConfiguration: String = "") {
        project.mkdirs()
        project.resolve("build.gradle.kts").writeText(
            """
            plugins { java }
            group = "example"
            repositories { mavenCentral() }
            dependencies { testImplementation("junit:junit:4.13.2") }
            tasks.test {
                useJUnit()
                reports.junitXml.outputLocation.set(layout.buildDirectory.dir("custom-junit"))
                $testConfiguration
            }
            """.trimIndent(),
        )
        val source = project.resolve("src/test/java/EvidenceTest.java")
        source.parentFile.mkdirs()
        source.writeText(
            """
            public class EvidenceTest {
                @org.junit.Test public void producesEvidence() { $testBody }
            }
            """.trimIndent(),
        )
    }

    private fun settings(project: File, name: String, script: String = "") {
        project.mkdirs()
        project.resolve("settings.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.dashboard-test-summary") }
            rootProject.name = "$name"
            """.trimIndent() + "\n" + script,
        )
    }

    private fun fixture(buildScript: String): GradleRunner {
        settings(directory, "fixture")
        directory.resolve("build.gradle.kts").writeText(buildScript)
        directory.resolve("first.xml").writeText(
            """<testsuite tests="99"><testcase/><testcase><error/></testcase><testcase><skipped/></testcase></testsuite>""",
        )
        directory.resolve("second.xml").writeText(
            """<testsuites><testsuite><testcase><failure/></testcase><testcase><property name="skip"/></testcase></testsuite></testsuites>""",
        )
        return runner()
    }

    private fun runner() = GradleRunner.create()
        .withProjectDir(directory)
        .withPluginClasspath()
        .withEnvironment(System.getenv() + mapOf("GITHUB_RUN_ID" to "42", "GITHUB_RUN_ATTEMPT" to "2", "GITHUB_SHA" to "abc"))
        .withArguments("dashboardTestSummary", "--continue", "--configuration-cache", "--console=plain")

    private fun summary() = Json.parseToJsonElement(directory.resolve("build/dashboard/test-summary.json").readText())
}
