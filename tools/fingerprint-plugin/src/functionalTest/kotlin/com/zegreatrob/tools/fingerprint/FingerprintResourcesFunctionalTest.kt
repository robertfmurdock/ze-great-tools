package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.junit.jupiter.api.Test

class FingerprintResourcesFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `fingerprint changes when main resources include a custom directory and its contents change`() = setup(object {
        val schema = fileUnderProject("graphql/schema.graphql")
        val resourceChange = """
            type Query { hello: String, goodbye: String }
        """.trimIndent()
    }) {
        writeSettings("java-custom-main-resources")

        writeJavaResourcesBuild(
            sourceSet = "main",
            directory = "graphql",
            note = "Proves we use Gradle's built-in SourceSet knowledge rather than hard-coded directories.",
        )

        schema.writeText(
            """
            type Query { hello: String }
            """.trimIndent(),
        )
    } exercise {
        runFingerprintTwiceAfter(change = { schema.writeText(resourceChange) })
    } verify { (baselineHash, afterChangeHash) ->
        assertFingerprintChanged(
            baselineHash,
            afterChangeHash,
            "Fingerprint should change when a main SourceSet resource (custom dir) changes!",
        )
    }

    @Test
    fun `fingerprint does not change when test resources include a custom directory and its contents change`() = setup(object {
        val fixture = fileUnderProject("test-fixtures/fixture.txt")
        val resourceChange = "v2"
    }) {
        writeSettings("java-custom-test-resources")

        writeJavaResourcesBuild(
            sourceSet = "test",
            directory = "test-fixtures",
            note = "Test resources should NOT affect the fingerprint.",
        )

        fixture.writeText("v1")
    } exercise {
        runFingerprintTwiceAfter(change = { fixture.writeText(resourceChange) })
    } verify { (baselineHash, afterChangeHash) ->
        assertFingerprintUnchanged(
            baselineHash,
            afterChangeHash,
            "Fingerprint should NOT change when test resources (custom dir) change!",
        )
    }

    @Test
    fun `fingerprint changes when KMP jsMain resources include a custom directory and its contents change`() = setup(object {
        val schema = fileUnderProject("graphql/schema.graphql")
        val resourceChange = """
            type Query { hello: String, goodbye: String }
        """.trimIndent()
    }) {
        writeSettings("kmp-js-custom-main-resources")

        writeKmpJsResourcesBuild(sourceSet = "jsMain", directory = "graphql")

        schema.writeText(
            """
            type Query { hello: String }
            """.trimIndent(),
        )
    } exercise {
        runFingerprintTwiceAfter(change = { schema.writeText(resourceChange) })
    } verify { (baselineHash, afterChangeHash) ->
        assertFingerprintChanged(
            baselineHash,
            afterChangeHash,
            "Fingerprint should change when KMP jsMain resources (custom dir) change!",
        )
    }

    @Test
    fun `fingerprint does not change when KMP JS test resources include a custom directory and its contents change`() = setup(object {
        val fixture = fileUnderProject("test-fixtures/fixture.txt")
        val resourceChange = "v2"
    }) {
        writeSettings("kmp-js-custom-test-resources")

        writeKmpJsResourcesBuild(sourceSet = "jsTest", directory = "test-fixtures")

        fixture.writeText("v1")
    } exercise {
        runFingerprintTwiceAfter(change = { fixture.writeText(resourceChange) })
    } verify { (baselineHash, afterChangeHash) ->
        assertFingerprintUnchanged(
            baselineHash,
            afterChangeHash,
            "Fingerprint should NOT change when KMP jsTest resources (custom dir) change!",
        )
    }

    private fun writeJavaResourcesBuild(sourceSet: String, directory: String, note: String) {
        writeBuild(
            """
            plugins {
                java
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }

            // $note
            sourceSets {
                named("$sourceSet") {
                    resources.srcDir("$directory")
                }
            }
            """.trimIndent(),
        )
    }

    private fun writeKmpJsResourcesBuild(sourceSet: String, directory: String) {
        writeBuild(
            kmpBuild(
                kotlinBlock = """
                    kotlin {
                        js(IR) { browser() }
                        sourceSets {
                            val $sourceSet by getting {
                                resources.srcDir("$directory")
                            }
                        }
                    }
                """.trimIndent(),
            ),
        )
    }
}
