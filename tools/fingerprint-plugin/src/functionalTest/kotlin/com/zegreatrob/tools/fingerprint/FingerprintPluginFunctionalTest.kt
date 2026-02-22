package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FingerprintPluginFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `plugin generates fingerprint file in KMP project`() = setup(object {
        val projectName = "kmp-test-project"
        val buildScript = kmpBuild(
            kotlinBlock = """
                kotlin {
                    jvm()
                }
            """.trimIndent(),
        )
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        gradle(arguments = arrayOf("generateFingerprint", "--configuration-cache"), forwardOutput = true)
    } verify {
        val fingerprintFile = fingerprintFile()
        assertTrue(fingerprintFile.exists(), "Fingerprint file should be generated at ${fingerprintFile.path}")
        assertFingerprintManifestGeneratedCorrectly()
    }

    @Test
    fun `fingerprint does not change when test source code is modified`() = setup(object {
        lateinit var testSourceFile: File
    }) {
        writeSettings("test-source-change-test")
        writeBuild(kmpBuild())

        testSourceFile = writeProjectFile(
            "src/commonTest/kotlin/ExampleTest.kt",
            """
            package example

            class ExampleTest {
                fun value(): Int = 1
            }
            """,
        )
    } exercise {
        val firstHash = runFingerprint()

        testSourceFile.writeText(
            """
            package example

            class ExampleTest {
                fun value(): Int = 2
            }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        Pair(firstHash, secondHash)
    } verify { (firstHash, secondHash) ->
        assertFingerprintUnchanged(firstHash, secondHash, "Fingerprint should NOT change when test sources change!")
    }

    @Test
    fun `fingerprint does not include junit when junit is test-only dependency`() = setup(object {
        val projectName = "junit-test-only-dependency-should-not-leak"
        val buildScript = """
            plugins {
                kotlin("jvm") version "2.3.0"
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
            }
        """.trimIndent()
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        runFingerprint()
        fingerprintManifestFile().readText()
    } verify { manifest ->
        assertManifestContainsDependencyIngredients(manifest, "sanity check: we should have at least one classpath entry")

        val classpathLines = manifest.lineSequence().filter { it.startsWith("classpath|") }.toList()
        val junitClasspathLines = classpathLines.filter { it.contains("junit", ignoreCase = true) }

        assertTrue(
            junitClasspathLines.isEmpty(),
            """
            JUnit must not be treated as a production dependency ingredient when it's only declared in testImplementation.
            Found classpath lines containing 'junit':
            ${junitClasspathLines.joinToString("\n")}
            
            Full manifest:
            $manifest
            """.trimIndent(),
        )
    }

    @Test
    fun `fingerprint changes when module source code is modified`() = setup(object {
        lateinit var sourceFile: File
    }) {
        writeSettings("source-change-test")
        writeBuild(kmpBuild())

        sourceFile = writeProjectFile(
            "src/commonMain/kotlin/Example.kt",
            """
            package example

            class Example {
                fun value(): Int = 1
            }
            """,
        )
    } exercise {
        val firstHash = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()

        sourceFile.writeText(
            """
            package example

            class Example {
                fun value(): Int = 2
            }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()
        val secondManifest = fingerprintManifestFile().readText()

        listOf(firstHash, secondHash, firstManifest, secondManifest)
    } verify { (firstHash, secondHash, firstManifest, secondManifest) ->
        assertTrue(firstManifest.contains("source|src/commonMain/kotlin/Example.kt|"))
        assertTrue(secondManifest.contains("source|src/commonMain/kotlin/Example.kt|"))
        assertFingerprintChanged(firstHash, secondHash, "Fingerprint should change when module source changes!")
    }

    @Test
    fun `fingerprint changes when dependencies are modified`() = setup(object {
        lateinit var base: String
    }) {
        writeSettings("dependency-test")

        base = kmpBuild()
        writeBuild(base)
    } exercise {
        val firstFingerprint = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()

        writeBuild(
            """
            $base

            kotlin {
                sourceSets {
                    val commonMain by getting {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val secondFingerprint = runFingerprint()
        val secondManifest = fingerprintManifestFile().readText()

        listOf(firstFingerprint, secondFingerprint, firstManifest, secondManifest)
    } verify { (firstFingerprint, secondFingerprint, firstManifest, secondManifest) ->
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")
        assertManifestContainsDependencyIngredients(secondManifest, context = "after dependency change")

        assertFingerprintChanged(firstFingerprint, secondFingerprint, "Fingerprint should have changed!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when non-test dependencies change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `fingerprint changes when JS dependencies are modified`() = setup(object {
        lateinit var base: String
    }) {
        writeSettings("js-test")

        base = kmpBuild(
            kotlinBlock = """
                kotlin {
                    js(IR) { browser() }
                }
            """.trimIndent(),
        )
        writeBuild(base)
    } exercise {
        val firstHash = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()

        writeBuild(
            """
            $base
            kotlin {
                sourceSets {
                    val jsMain by getting {
                        dependencies {
                            implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.9.1")
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()
        val secondManifest = fingerprintManifestFile().readText()

        listOf(firstHash, secondHash, firstManifest, secondManifest)
    } verify { (firstHash, secondHash, firstManifest, secondManifest) ->
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")
        assertManifestContainsDependencyIngredients(secondManifest, context = "after dependency change")

        assertFingerprintChanged(firstHash, secondHash, "JS fingerprint should have changed!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when JS main dependencies change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `fingerprint remains identical when test dependencies change`() = setup(object {
        lateinit var base: String
    }) {
        writeSettings("test-isolation-check")

        base = kmpBuild()
        writeBuild(base)
    } exercise {
        val firstHash = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()

        writeBuild(
            """
            $base
            kotlin {
                sourceSets {
                    val commonTest by getting {
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-test")
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()
        val secondManifest = fingerprintManifestFile().readText()

        listOf(firstHash, secondHash, firstManifest, secondManifest)
    } verify { (firstHash, secondHash, firstManifest, secondManifest) ->
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")
        assertManifestContainsDependencyIngredients(secondManifest, context = "after test dependency change")

        assertFingerprintUnchanged(firstHash, secondHash, "Fingerprint should NOT change for test dependencies!")
        assertEquals(
            firstManifest,
            secondManifest,
            "Manifest should NOT change for test-only dependency changes.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `fingerprint changes when plugin version changes`() = setup(object {
        val baseBuildScript = kmpBuild()
    }) {
        testProjectDir.resolve("settings.gradle.kts").writeText("")
        testProjectDir.resolve("build.gradle.kts").writeText(baseBuildScript)
    } exercise {
        val hashA = runWithVersion(testProjectDir, "1.0.0")
        val hashB = runWithVersion(testProjectDir, "2.0.0")
        Pair(hashA, hashB)
    } verify { (hashA, hashB) ->
        assert(hashA != hashB) { "Fingerprint must change when plugin version updates!" }
    }

    private fun runWithVersion(dir: File, version: String): String {
        gradle(dir, "generateFingerprint", "-Dtest.plugin.version=$version")
        assertFingerprintManifestGeneratedCorrectly(dir, expectedPluginVersion = version)
        return fingerprintFile(dir).readText()
    }

    @Test
    fun `task is retrieved from build cache on second run`() = setup(object {
        val buildScript = kmpBuild()
    }) {
        writeProject(buildScript = buildScript)
    } exercise {
        gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))
        val firstManifest = fingerprintManifestFile().readText()

        testProjectDir.resolve("build").deleteRecursively()

        val result = gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))
        val secondManifest = fingerprintManifestFile().readText()

        CacheRunResult(result, firstManifest, secondManifest)
    } verify { data ->
        assertTrue(data.firstManifest.isNotBlank())
        assertTrue(data.secondManifest.isNotBlank())
        assertEquals(data.result.task(":generateFingerprint")?.outcome, TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `root fingerprint reflects changes in subprojects`() = setup(object {
        lateinit var appBuildFile: File
    }) {
        settingsFile.writeText(
            """
            rootProject.name = "multi-project-root"
            include(":app")
            """.trimIndent(),
        )

        buildFile.writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint"); kotlin("multiplatform") version "2.3.0" apply false }
            """.trimIndent(),
        )

        appBuildFile = testProjectDir.resolve("app/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins { kotlin("multiplatform") version "2.3.0" }
                kotlin { jvm() }
                repositories { mavenCentral() }
                
                """.trimIndent(),
            )
        }
    } exercise {
        val firstHash = runFingerprint()

        appBuildFile.appendText(
            """
            dependencies { "commonMainImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        Pair(firstHash, secondHash)
    } verify { (firstHash, secondHash) ->
        assert(firstHash != secondHash) { "Root fingerprint must change when subproject dependencies change!" }
    }

    @Test
    fun `fingerprint ignores unconfigured subprojects`() = setup(object {
        lateinit var appBuild: File
        lateinit var ignoredBuild: File
    }) {
        settingsFile.writeText(
            """
            rootProject.name = "filter-test"
            include(":app", ":ignored-lib")
            """.trimIndent(),
        )

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint"); kotlin("jvm") version "2.3.0" apply false }
            repositories { mavenCentral() }
            fingerprintConfig {
                includedProjects.add("app")
            }
            """.trimIndent(),
        )

        appBuild = testProjectDir.resolve("app/build.gradle.kts").apply { parentFile.mkdirs() }
        ignoredBuild = testProjectDir.resolve("ignored-lib/build.gradle.kts").apply { parentFile.mkdirs() }

        appBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")
        ignoredBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")
    } exercise {
        val firstHash = runFingerprint()

        ignoredBuild.appendText("\ndependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3\") }")
        val secondHash = runFingerprint()

        appBuild.appendText("\ndependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3\") }")
        val thirdHash = runFingerprint()

        listOf(firstHash, secondHash, thirdHash)
    } verify { (firstHash, secondHash, thirdHash) ->
        assertFingerprintUnchanged(firstHash, secondHash, "Hash should remain stable when unconfigured subprojects change!")
        assertFingerprintChanged(firstHash, thirdHash, "Hash should change when configured subprojects change!")
    }

    @Test
    fun `fingerprint fails if dependencies cannot be resolved`() = setup(object {
        val projectName = "resolution-failure-test"
        val buildScript = """
            plugins {
                kotlin("jvm") version "2.3.0"
                id("com.zegreatrob.tools.fingerprint")
            }
            // NO repositories defined
            dependencies {
                implementation("com.example:fake-lib:1.0.0")
            }
        """.trimIndent()
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        gradle(arguments = arrayOf("generateFingerprint"), expectFailure = true)
    } verify { result ->
        assertTrue(result.output.contains("Could not resolve"), result.output)
    }

    @Test
    fun `aggregateFingerprints combines hashes from included builds`() = setup(object {
        lateinit var includedDir: File
        lateinit var mainDir: File
    }) {
        includedDir = testProjectDir.resolve("my-included-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-included-lib"""")

        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            val includedMarker = file("included-marker.txt").apply { writeText("lib-content") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("1.0")
                classpath.setFrom(files(includedMarker))
            }
            """.trimIndent(),
        )

        mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "main-app"
            includeBuild("../my-included-lib")
            """.trimIndent(),
        )

        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint"); kotlin("multiplatform") version "2.3.0" apply false }

            val mainMarker = file("main-marker.txt").apply { writeText("app-content") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("1.0")
                classpath.setFrom(files(mainMarker))
            }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, ":my-included-lib:generateFingerprint", "aggregateFingerprints")
    } verify {
        val localHash = fingerprintFile(mainDir).readText()
        val includedHash = fingerprintFile(includedDir).readText()
        val aggregateHash = mainDir.resolve("build/aggregate-fingerprint.txt").readText()

        assert(aggregateHash != localHash) { "Aggregate should not match local hash" }
        assert(aggregateHash != includedHash) { "Aggregate should not match included hash" }
    }

    @Test
    fun `aggregateFingerprints triggers generateFingerprint in included builds`() = setup(object {
        lateinit var mainDir: File
    }) {
        val includedDir = testProjectDir.resolve("my-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-lib"""")
        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )

        mainDir = testProjectDir.resolve("my-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "my-app"
            includeBuild("../my-lib")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            fingerprintConfig {
                includedBuilds.add("my-lib")
            }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, "aggregateFingerprints")
    } verify { result ->
        val includedTaskResult = result.task(":my-lib:generateFingerprint")
        assertNotNull(includedTaskResult, "Task in included build should have been triggered")
        assertTrue(
            includedTaskResult.outcome == TaskOutcome.SUCCESS || includedTaskResult.outcome == TaskOutcome.UP_TO_DATE,
            "Included task should have executed successfully",
        )
    }

    @Test
    fun `aggregateFingerprints skips included builds that do not have the plugin`() = setup(object {
        lateinit var mainDir: File
    }) {
        val nakedDir = testProjectDir.resolve("naked-lib").apply { mkdirs() }
        nakedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "naked-lib"""")
        nakedDir.resolve("build.gradle.kts").writeText("// Empty - no plugin here")

        mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "main-app"
            includeBuild("../naked-lib")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, "aggregateFingerprints", "--stacktrace", "--no-configuration-cache")
    } verify { result ->
        assertTrue(result.output.contains("SUCCESS"), "Build should succeed even with non-plugin included builds")
        val aggregateFile = mainDir.resolve("build/aggregate-fingerprint.txt")
        assertTrue(aggregateFile.exists(), "Aggregate file should still be generated")
    }

    @Test
    fun `fingerprint changes when main resources include a custom directory and its contents change`() = setup(object {
        lateinit var schema: File
    }) {
        writeSettings("java-custom-main-resources")

        writeBuild(
            """
            plugins {
                java
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }

            // Proves we use Gradle's built-in SourceSet knowledge rather than hard-coded directories.
            sourceSets {
                named("main") {
                    resources.srcDir("graphql")
                }
            }
            """.trimIndent(),
        )

        schema = writeProjectFile(
            "graphql/schema.graphql",
            """
            type Query { hello: String }
            """,
        )
    } exercise {
        val firstHash = runFingerprint()

        schema.writeText(
            """
            type Query { hello: String, goodbye: String }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        Pair(firstHash, secondHash)
    } verify { (firstHash, secondHash) ->
        assertFingerprintChanged(
            firstHash,
            secondHash,
            "Fingerprint should change when a main SourceSet resource (custom dir) changes!",
        )
    }

    @Test
    fun `fingerprint does not change when test resources include a custom directory and its contents change`() = setup(object {
        lateinit var fixture: File
    }) {
        writeSettings("java-custom-test-resources")

        writeBuild(
            """
            plugins {
                java
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }

            // Test resources should NOT affect the fingerprint.
            sourceSets {
                named("test") {
                    resources.srcDir("test-fixtures")
                }
            }
            """.trimIndent(),
        )

        fixture = writeProjectFile("test-fixtures/fixture.txt", "v1")
    } exercise {
        val firstHash = runFingerprint()

        fixture.writeText("v2")

        val secondHash = runFingerprint()

        Pair(firstHash, secondHash)
    } verify { (firstHash, secondHash) ->
        assertFingerprintUnchanged(
            firstHash,
            secondHash,
            "Fingerprint should NOT change when test resources (custom dir) change!",
        )
    }

    @Test
    fun `fingerprint changes when KMP jsMain resources include a custom directory and its contents change`() = setup(object {
        lateinit var schema: File
    }) {
        writeSettings("kmp-js-custom-main-resources")

        writeBuild(
            kmpBuild(
                kotlinBlock = """
                    kotlin {
                        js(IR) { browser() }
                        sourceSets {
                            val jsMain by getting {
                                resources.srcDir("graphql")
                            }
                        }
                    }
                """.trimIndent(),
            ),
        )

        schema = writeProjectFile(
            "graphql/schema.graphql",
            """
            type Query { hello: String }
            """,
        )
    } exercise {
        val firstHash = runFingerprint()

        schema.writeText(
            """
            type Query { hello: String, goodbye: String }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        Pair(firstHash, secondHash)
    } verify { (firstHash, secondHash) ->
        assertFingerprintChanged(firstHash, secondHash, "Fingerprint should change when KMP jsMain resources (custom dir) change!")
    }

    @Test
    fun `fingerprint does not change when KMP JS test resources include a custom directory and its contents change`() = setup(object {
        lateinit var fixture: File
    }) {
        writeSettings("kmp-js-custom-test-resources")

        writeBuild(
            kmpBuild(
                kotlinBlock = """
                    kotlin {
                        js(IR) { browser() }
                        sourceSets {
                            val jsTest by getting {
                                resources.srcDir("test-fixtures")
                            }
                        }
                    }
                """.trimIndent(),
            ),
        )

        fixture = writeProjectFile("test-fixtures/fixture.txt", "v1")
    } exercise {
        val firstHash = runFingerprint()

        fixture.writeText("v2")

        val secondHash = runFingerprint()

        Pair(firstHash, secondHash)
    } verify { (firstHash, secondHash) ->
        assertFingerprintUnchanged(
            firstHash,
            secondHash,
            "Fingerprint should NOT change when KMP jsTest resources (custom dir) change!",
        )
    }

    @Test
    fun `fingerprint handles classpath entries that are directories and changes when directory contents change`() = setup(object {
        lateinit var marker: File
    }) {
        writeSettings("classpath-directory-entry-test")

        val classpathDir = fileUnderProject("build/classes/kotlin/js/main").apply { mkdirs() }
        marker = classpathDir.resolve("marker.txt").apply { writeText("v1") }

        writeBuild(
            """
            plugins {
                id("com.zegreatrob.tools.fingerprint")
            }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("1.0")
                classpath.setFrom(files("${classpathDir.invariantSeparatorsPath}"))
            }
            """.trimIndent(),
        )
    } exercise {
        val firstHash = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()

        marker.writeText("v2")

        val secondHash = runFingerprint()
        val secondManifest = fingerprintManifestFile().readText()

        listOf(firstHash, secondHash, firstManifest, secondManifest)
    } verify { (firstHash, secondHash, firstManifest, secondManifest) ->
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline (directory classpath entry)")
        assertManifestContainsDependencyIngredients(secondManifest, context = "after directory content change")

        assertFingerprintChanged(firstHash, secondHash, "Fingerprint should change when a directory classpath entry contents change!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when a directory classpath entry contents change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `aggregateFingerprints merges included + local manifest logs into one aggregate manifest log file`() = setup(object {
        lateinit var includedDir: File
        lateinit var mainDir: File
    }) {
        includedDir = testProjectDir.resolve("my-included-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-included-lib"""")
        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            val includedMarker = file("included-marker.txt").apply { writeText("included-content") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("included-1.0")
                classpath.setFrom(files(includedMarker))
            }
            """.trimIndent(),
        )

        mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
        mainDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "main-app"
            includeBuild("../my-included-lib")
            """.trimIndent(),
        )
        mainDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            fingerprintConfig {
                includedBuilds.add("my-included-lib")
            }

            val mainMarker = file("main-marker.txt").apply { writeText("main-content") }

            tasks.named<com.zegreatrob.tools.fingerprint.FingerprintTask>("generateFingerprint") {
                pluginVersion.set("main-1.0")
                classpath.setFrom(files(mainMarker))
            }
            """.trimIndent(),
        )
    } exercise {
        gradle(mainDir, "aggregateFingerprints", "--no-configuration-cache")
    } verify {
        assertTrue(
            aggregateFingerprintFile(mainDir).exists(),
            "Aggregate fingerprint file should be generated at ${aggregateFingerprintFile(mainDir).path}",
        )

        val aggregateManifest = aggregateFingerprintManifestFile(mainDir)
        assertTrue(
            aggregateManifest.exists(),
            "Aggregate manifest log should be generated at ${aggregateManifest.path}",
        )

        val text = aggregateManifest.readText()

        assertTrue(
            text.contains("included-marker.txt"),
            "Aggregate manifest should include evidence from the included build manifest.\n--- aggregate ---\n$text",
        )
        assertTrue(
            text.contains("main-marker.txt"),
            "Aggregate manifest should include evidence from the local/main manifest.\n--- aggregate ---\n$text",
        )

        val pluginVersionLines = text.lineSequence().filter { it.startsWith("pluginVersion|") }.toList()
        assertTrue(
            pluginVersionLines.size >= 2,
            "Aggregate manifest should contain pluginVersion lines from multiple manifests. Found=${pluginVersionLines.size}\n--- aggregate ---\n$text",
        )
    }

    @Test
    fun `aggregateFingerprints works when includedBuilds is not configured`() = setup(object {
        val projectName = "aggregate-defaults-test"
        val buildScript = """
            plugins { id("com.zegreatrob.tools.fingerprint") }
        """.trimIndent()
    }) {
        writeProject(projectName, buildScript)
    } exercise {
        gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))
    } verify { result ->
        assertEquals(TaskOutcome.SUCCESS, result.task(":aggregateFingerprints")?.outcome)

        assertTrue(fingerprintFile().exists(), "Local fingerprint should be generated at ${fingerprintFile().path}")
        assertFingerprintManifestGeneratedCorrectly()

        assertTrue(
            aggregateFingerprintFile(testProjectDir).exists(),
            "Aggregate fingerprint file should be generated at ${aggregateFingerprintFile(testProjectDir).path}",
        )
        assertTrue(
            aggregateFingerprintManifestFile(testProjectDir).exists(),
            "Aggregate manifest log should be generated at ${aggregateFingerprintManifestFile(testProjectDir).path}",
        )
    }

    @Test
    fun `fingerprint changes when published artifact bytes change even if sources and dependencies do not`() = setup(object {
        lateinit var buildWithManifest: (String) -> String
    }) {
        writeSettings("published-artifact-change-test")

        // Stable main source (unchanged between runs)
        writeProjectFile(
            "src/main/java/example/Hello.java",
            """
            package example;

            public class Hello {
                public static String value() { return "hello"; }
            }
            """,
        )

        buildWithManifest = { implementationVersion ->
            """
            plugins {
                java
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }

            tasks.jar {
                manifest {
                    attributes["Implementation-Version"] = "$implementationVersion"
                }
            }
            """.trimIndent()
        }
    } exercise {
        writeBuild(buildWithManifest("1"))
        val hash1 = runFingerprint()

        testProjectDir.resolve("build").deleteRecursively()

        writeBuild(buildWithManifest("2"))
        val hash2 = runFingerprint()

        Pair(hash1, hash2)
    } verify { (hash1, hash2) ->
        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when the produced JAR bytes change (e.g., manifest attribute change), " +
                "even if sources and dependencies are unchanged.",
        )
    }

    @Test
    fun `fingerprint changes when KMP published artifact bytes change even if sources and dependencies do not`() = setup(object {
        lateinit var buildWithJvmJarManifest: (String) -> String
    }) {
        writeSettings("kmp-published-artifact-change-test")

        writeProjectFile(
            "src/commonMain/kotlin/example/Hello.kt",
            """
            package example

            class Hello {
                fun value(): String = "hello"
            }
            """,
        )

        buildWithJvmJarManifest = { implementationVersion ->
            """
            plugins {
                kotlin("multiplatform") version "2.3.0"
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }

            kotlin { jvm() }

            tasks.named<org.gradle.jvm.tasks.Jar>("jvmJar") {
                manifest {
                    attributes["Implementation-Version"] = "$implementationVersion"
                }
            }
            """.trimIndent()
        }
    } exercise {
        writeBuild(buildWithJvmJarManifest("1"))
        val hash1 = runFingerprint()

        testProjectDir.resolve("build").deleteRecursively()

        writeBuild(buildWithJvmJarManifest("2"))
        val hash2 = runFingerprint()

        Pair(hash1, hash2)
    } verify { (hash1, hash2) ->
        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when a KMP published artifact (jvmJar) bytes change, even if sources/deps are unchanged.",
        )
    }

    @Test
    fun `fingerprint changes when build logic changes via buildSrc plugin implementation change`() = setup(object {
        lateinit var writeNoOpPluginSource: (String) -> Unit
    }) {
        writeSettings("build-logic-change-test")

        writeProjectFile(
            "src/main/java/example/Hello.java",
            """
            package example;

            public class Hello {
                public static String value() { return "hello"; }
            }
            """,
        )

        writeProjectFile(
            "buildSrc/build.gradle.kts",
            """
            plugins {
                `kotlin-dsl`
            }

            repositories {
                gradlePluginPortal()
                mavenCentral()
            }
            """,
        )

        writeProjectFile(
            "buildSrc/src/main/resources/META-INF/gradle-plugins/test.noop.properties",
            """
            implementation-class=example.NoOpPlugin
            """,
        )

        writeNoOpPluginSource = { marker ->
            writeProjectFile(
                "buildSrc/src/main/kotlin/example/NoOpPlugin.kt",
                """
                package example

                import org.gradle.api.Plugin
                import org.gradle.api.Project
                import org.gradle.api.tasks.bundling.Jar

                class NoOpPlugin : Plugin<Project> {
                    override fun apply(target: Project) {
                        // Receiver lambda form (Kotlin DSL): no parameters here.
                        target.tasks.withType(Jar::class.java).configureEach {
                            manifest.attributes(mapOf("Implementation-Version" to "$marker"))
                        }
                    }
                }
                """,
            )
        }

        writeNoOpPluginSource("A")

        writeBuild(
            """
            plugins {
                java
                id("test.noop")
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }
            """.trimIndent(),
        )
    } exercise {
        val hash1 = runFingerprint()

        writeNoOpPluginSource("B")

        testProjectDir.resolve("build").deleteRecursively()

        val hash2 = runFingerprint()

        Pair(hash1, hash2)
    } verify { (hash1, hash2) ->
        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when build logic (buildSrc plugin code) changes AND that change affects produced artifact bytes.",
        )
    }

    @Test
    fun `fingerprint changes when build logic changes via buildSrc convention plugin for KMP`() = setup(object {
        lateinit var writeConventionPluginSource: (String) -> Unit
    }) {
        writeSettings("build-logic-kmp-change-test")

        writeProjectFile(
            "src/commonMain/kotlin/example/Hello.kt",
            """
            package example

            class Hello {
                fun value(): String = "hello"
            }
            """,
        )

        writeProjectFile(
            "buildSrc/build.gradle.kts",
            """
            plugins { `kotlin-dsl` }

            repositories {
                gradlePluginPortal()
                mavenCentral()
            }
            """,
        )

        writeProjectFile(
            "buildSrc/src/main/resources/META-INF/gradle-plugins/test.kmpconvention.properties",
            """
            implementation-class=example.KmpConventionPlugin
            """,
        )

        writeConventionPluginSource = { marker ->
            writeProjectFile(
                "buildSrc/src/main/kotlin/example/KmpConventionPlugin.kt",
                """
                package example

                import org.gradle.api.Plugin
                import org.gradle.api.Project
                import org.gradle.api.tasks.bundling.Jar

                class KmpConventionPlugin : Plugin<Project> {
                    override fun apply(target: Project) {
                        target.tasks.withType(Jar::class.java)
                            .matching { it.name == "jvmJar" }
                            .configureEach {
                                manifest.attributes(mapOf("Implementation-Version" to "$marker"))
                            }
                    }
                }
                """,
            )
        }

        writeConventionPluginSource("A")

        writeBuild(
            """
            plugins {
                kotlin("multiplatform") version "2.3.0"
                id("test.kmpconvention")
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }

            kotlin { jvm() }
            """.trimIndent(),
        )
    } exercise {
        fun assertManifestShowsJvmJarWasFingerprinted(context: String) {
            val manifest = fingerprintManifestFile().readText()
            assertTrue(
                manifest.lineSequence().any { it.startsWith("artifact|") && it.contains("jvm", ignoreCase = true) && it.contains("jar", ignoreCase = true) },
                "Manifest must include an artifact line for the KMP JVM jar ($context). Manifest:\n$manifest",
            )
        }

        val hash1 = runFingerprint()
        assertManifestShowsJvmJarWasFingerprinted("first run")

        writeConventionPluginSource("B")
        testProjectDir.resolve("build").deleteRecursively()

        val hash2 = runFingerprint()
        assertManifestShowsJvmJarWasFingerprinted("second run")

        Pair(hash1, hash2)
    } verify { (hash1, hash2) ->
        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when KMP convention build logic changes AND that change affects published artifact bytes (jvmJar).",
        )
    }

    @Test
    fun `compareAggregateFingerprints succeeds and prints bash-friendly match indicator when fingerprints are equal`() = setup(object {
        lateinit var expectedFile: File
    }) {
        writeSettings("compare-aggregate-fingerprints-success")

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            fingerprintConfig {
                compareToFile.set(layout.projectDirectory.file("expected/aggregate-fingerprint.txt"))
            }
            """.trimIndent(),
        )

        expectedFile = fileUnderProject("expected/aggregate-fingerprint.txt")
    } exercise {
        val aggregateResult = gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))

        expectedFile.writeText(aggregateFingerprintFile(testProjectDir).readText())

        val compareResult = gradle(arguments = arrayOf("compareAggregateFingerprints", "--no-configuration-cache"))

        listOf(aggregateResult, compareResult)
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
        lateinit var expectedFile: File
    }) {
        writeSettings("compare-aggregate-fingerprints-failure")

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }

            fingerprintConfig {
                compareToFile.set(layout.projectDirectory.file("expected/aggregate-fingerprint.txt"))
            }
            """.trimIndent(),
        )

        expectedFile = fileUnderProject("expected/aggregate-fingerprint.txt")
    } exercise {
        val aggregateResult = gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))

        expectedFile.writeText("definitely-not-the-real-fingerprint")

        val compareResult = gradle(
            arguments = arrayOf("compareAggregateFingerprints", "--no-configuration-cache"),
            expectFailure = true,
        )

        listOf(aggregateResult, compareResult)
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
        lateinit var expectedFile: File
    }) {
        writeSettings("compare-aggregate-fingerprints-property-config")

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )

        expectedFile = fileUnderProject("expected/aggregate-fingerprint.txt")
    } exercise {
        val aggregateResult = gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))

        expectedFile.writeText(aggregateFingerprintFile(testProjectDir).readText())

        val compareResult = gradle(
            arguments = arrayOf(
                "compareAggregateFingerprints",
                "--no-configuration-cache",
                "-PfingerprintCompareToFile=expected/aggregate-fingerprint.txt",
            ),
        )

        listOf(aggregateResult, compareResult)
    } verify { (aggregateResult, compareResult) ->
        assertEquals(TaskOutcome.SUCCESS, aggregateResult.task(":aggregateFingerprints")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, compareResult.task(":compareAggregateFingerprints")?.outcome)
        assertTrue(
            compareResult.output.lineSequence().any { it.trim() == "FINGERPRINT_MATCH=true" },
            "Expected `FINGERPRINT_MATCH=true` in output.\n--- output ---\n${compareResult.output}",
        )
    }

    private data class CacheRunResult(
        val result: BuildResult,
        val firstManifest: String,
        val secondManifest: String,
    )
}
