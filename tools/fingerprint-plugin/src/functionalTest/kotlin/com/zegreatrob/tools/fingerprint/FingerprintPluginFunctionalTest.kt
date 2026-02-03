package com.zegreatrob.tools.fingerprint

import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FingerprintPluginFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `plugin generates fingerprint file in KMP project`() {
        writeSettings("kmp-test-project")
        writeBuild(
            kmpBuild(
                kotlinBlock = """
                    kotlin {
                        jvm()
                    }
                """.trimIndent(),
            ),
        )

        gradle(arguments = arrayOf("generateFingerprint", "--configuration-cache"), forwardOutput = true)

        val fingerprintFile = fingerprintFile()
        assertTrue(fingerprintFile.exists(), "Fingerprint file should be generated at ${fingerprintFile.path}")
        assertFingerprintManifestGeneratedCorrectly()
    }

    @Test
    fun `fingerprint does not change when test source code is modified`() {
        writeSettings("test-source-change-test")
        writeBuild(kmpBuild())

        val testSourceFile = writeProjectFile(
            "src/commonTest/kotlin/ExampleTest.kt",
            """
            package example

            class ExampleTest {
                fun value(): Int = 1
            }
            """,
        )

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

        assertFingerprintUnchanged(firstHash, secondHash, "Fingerprint should NOT change when test sources change!")
    }

    @Test
    fun `fingerprint does not include junit when junit is test-only dependency`() {
        writeSettings("junit-test-only-dependency-should-not-leak")

        writeBuild(
            """
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
            """,
        )

        runFingerprint()

        val manifest = fingerprintManifestFile().readText()
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
    fun `fingerprint changes when module source code is modified`() {
        writeSettings("source-change-test")
        writeBuild(kmpBuild())

        val sourceFile = writeProjectFile(
            "src/commonMain/kotlin/Example.kt",
            """
            package example

            class Example {
                fun value(): Int = 1
            }
            """,
        )

        val firstHash = runFingerprint()
        assertFingerprintManifestGeneratedCorrectly(expectedSourcePaths = arrayOf("src/commonMain/kotlin/Example.kt"))

        sourceFile.writeText(
            """
            package example

            class Example {
                fun value(): Int = 2
            }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()
        assertFingerprintManifestGeneratedCorrectly(expectedSourcePaths = arrayOf("src/commonMain/kotlin/Example.kt"))

        assertFingerprintChanged(firstHash, secondHash, "Fingerprint should change when module source changes!")
    }

    @Test
    fun `fingerprint changes when dependencies are modified`() {
        writeSettings("dependency-test")

        val base = kmpBuild()
        writeBuild(base)

        val firstFingerprint = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")

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
        assertManifestContainsDependencyIngredients(secondManifest, context = "after dependency change")

        assertFingerprintChanged(firstFingerprint, secondFingerprint, "Fingerprint should have changed!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when non-test dependencies change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `fingerprint changes when JS dependencies are modified`() {
        writeSettings("js-test")

        val base = kmpBuild(
            kotlinBlock = """
                kotlin {
                    js(IR) { browser() }
                }
            """.trimIndent(),
        )
        writeBuild(base)

        val firstHash = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")

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
        assertManifestContainsDependencyIngredients(secondManifest, context = "after dependency change")

        assertFingerprintChanged(firstHash, secondHash, "JS fingerprint should have changed!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when JS main dependencies change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `fingerprint remains identical when test dependencies change`() {
        writeSettings("test-isolation-check")

        val base = kmpBuild()
        writeBuild(base)

        val firstHash = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline")

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
        assertManifestContainsDependencyIngredients(secondManifest, context = "after test dependency change")

        assertFingerprintUnchanged(firstHash, secondHash, "Fingerprint should NOT change for test dependencies!")
        assertEquals(
            firstManifest,
            secondManifest,
            "Manifest should NOT change for test-only dependency changes.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `fingerprint changes when plugin version changes`(@TempDir testProjectDir: File) {
        val buildFile = testProjectDir.resolve("build.gradle.kts")
        testProjectDir.resolve("settings.gradle.kts").writeText("")

        val baseBuildScript = kmpBuild()
        buildFile.writeText(baseBuildScript)

        val hashA = runWithVersion(testProjectDir, "1.0.0")
        val hashB = runWithVersion(testProjectDir, "2.0.0")

        assert(hashA != hashB) { "Fingerprint must change when plugin version updates!" }
    }

    private fun runWithVersion(dir: File, version: String): String {
        gradle(dir, "generateFingerprint", "-Dtest.plugin.version=$version")
        assertFingerprintManifestGeneratedCorrectly(dir, expectedPluginVersion = version)
        return fingerprintFile(dir).readText()
    }

    @Test
    fun `task is retrieved from build cache on second run`() {
        writeBuild(kmpBuild())

        gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))
        assertFingerprintManifestGeneratedCorrectly()

        testProjectDir.resolve("build").deleteRecursively()

        val result = gradle(arguments = arrayOf("generateFingerprint", "--build-cache"))
        assertFingerprintManifestGeneratedCorrectly()

        assertEquals(result.task(":generateFingerprint")?.outcome, TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `root fingerprint reflects changes in subprojects`() {
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

        val appBuildFile = testProjectDir.resolve("app/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins { kotlin("multiplatform") version "2.3.0" }
                kotlin { jvm() }
                repositories { mavenCentral() }
                
                """.trimIndent(),
            )
        }

        val firstHash = runFingerprint()

        appBuildFile.appendText(
            """
            dependencies { "commonMainImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        assert(firstHash != secondHash) { "Root fingerprint must change when subproject dependencies change!" }
    }

    @Test
    fun `fingerprint ignores unconfigured subprojects`() {
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

        val appBuild = testProjectDir.resolve("app/build.gradle.kts").apply { parentFile.mkdirs() }
        val ignoredBuild = testProjectDir.resolve("ignored-lib/build.gradle.kts").apply { parentFile.mkdirs() }

        appBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")
        ignoredBuild.writeText("""plugins { kotlin("jvm") version "2.3.0" } repositories { mavenCentral() }""")

        val firstHash = runFingerprint()

        ignoredBuild.appendText("\ndependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3\") }")
        val secondHash = runFingerprint()

        assertFingerprintUnchanged(firstHash, secondHash, "Hash should remain stable when unconfigured subprojects change!")

        appBuild.appendText("\ndependencies { implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3\") }")
        val thirdHash = runFingerprint()

        assertFingerprintChanged(firstHash, thirdHash, "Hash should change when configured subprojects change!")
    }

    @Test
    fun `fingerprint fails if dependencies cannot be resolved`() {
        writeSettings("resolution-failure-test")
        writeBuild(
            """
            plugins {
                kotlin("jvm") version "2.3.0"
                id("com.zegreatrob.tools.fingerprint")
            }
            // NO repositories defined
            dependencies {
                implementation("com.example:fake-lib:1.0.0")
            }
            """.trimIndent(),
        )

        val result = gradle(arguments = arrayOf("generateFingerprint"), expectFailure = true)

        assertTrue(result.output.contains("Could not resolve"), result.output)
    }

    @Test
    fun `aggregateFingerprints combines hashes from included builds`(@TempDir testProjectDir: File) {
        val includedDir = testProjectDir.resolve("my-included-lib").apply { mkdirs() }
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

        val mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
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

        gradle(mainDir, ":my-included-lib:generateFingerprint", "aggregateFingerprints")

        val localHash = fingerprintFile(mainDir).readText()
        val includedHash = fingerprintFile(includedDir).readText()
        val aggregateHash = mainDir.resolve("build/aggregate-fingerprint.txt").readText()

        assert(aggregateHash != localHash) { "Aggregate should not match local hash" }
        assert(aggregateHash != includedHash) { "Aggregate should not match included hash" }
    }

    @Test
    fun `aggregateFingerprints triggers generateFingerprint in included builds`(@TempDir testProjectDir: File) {
        val includedDir = testProjectDir.resolve("my-lib").apply { mkdirs() }
        includedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-lib"""")
        includedDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """.trimIndent(),
        )

        val mainDir = testProjectDir.resolve("my-app").apply { mkdirs() }
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

        val result = gradle(mainDir, "aggregateFingerprints")

        val includedTaskResult = result.task(":my-lib:generateFingerprint")
        assertNotNull(includedTaskResult, "Task in included build should have been triggered")
        assertTrue(
            includedTaskResult.outcome == TaskOutcome.SUCCESS || includedTaskResult.outcome == TaskOutcome.UP_TO_DATE,
            "Included task should have executed successfully",
        )
    }

    @Test
    fun `aggregateFingerprints skips included builds that do not have the plugin`(@TempDir testProjectDir: File) {
        val nakedDir = testProjectDir.resolve("naked-lib").apply { mkdirs() }
        nakedDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "naked-lib"""")
        nakedDir.resolve("build.gradle.kts").writeText("// Empty - no plugin here")

        val mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
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

        val result = gradle(mainDir, "aggregateFingerprints", "--stacktrace", "--no-configuration-cache")

        assertTrue(result.output.contains("SUCCESS"), "Build should succeed even with non-plugin included builds")
        val aggregateFile = mainDir.resolve("build/aggregate-fingerprint.txt")
        assertTrue(aggregateFile.exists(), "Aggregate file should still be generated")
    }

    @Test
    fun `fingerprint changes when main resources include a custom directory and its contents change`() {
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

        val schema = writeProjectFile(
            "graphql/schema.graphql",
            """
            type Query { hello: String }
            """,
        )

        val firstHash = runFingerprint()

        schema.writeText(
            """
            type Query { hello: String, goodbye: String }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        assertFingerprintChanged(
            firstHash,
            secondHash,
            "Fingerprint should change when a main SourceSet resource (custom dir) changes!",
        )
    }

    @Test
    fun `fingerprint does not change when test resources include a custom directory and its contents change`() {
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

        val fixture = writeProjectFile("test-fixtures/fixture.txt", "v1")

        val firstHash = runFingerprint()

        fixture.writeText("v2")

        val secondHash = runFingerprint()

        assertFingerprintUnchanged(
            firstHash,
            secondHash,
            "Fingerprint should NOT change when a test SourceSet resource (custom dir) changes!",
        )
    }

    @Test
    fun `fingerprint changes when KMP JS main resources include a custom directory and its contents change`() {
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

        val schema = writeProjectFile(
            "graphql/schema.graphql",
            """
            type Query { hello: String }
            """,
        )

        val firstHash = runFingerprint()

        schema.writeText(
            """
            type Query { hello: String, goodbye: String }
            """.trimIndent(),
        )

        val secondHash = runFingerprint()

        assertFingerprintChanged(firstHash, secondHash, "Fingerprint should change when KMP jsMain resources (custom dir) change!")
    }

    @Test
    fun `fingerprint does not change when KMP JS test resources include a custom directory and its contents change`() {
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

        val fixture = writeProjectFile("test-fixtures/fixture.txt", "v1")

        val firstHash = runFingerprint()

        fixture.writeText("v2")

        val secondHash = runFingerprint()

        assertFingerprintUnchanged(
            firstHash,
            secondHash,
            "Fingerprint should NOT change when KMP jsTest resources (custom dir) change!",
        )
    }

    @Test
    fun `fingerprint handles classpath entries that are directories and changes when directory contents change`() {
        writeSettings("classpath-directory-entry-test")

        val classpathDir = fileUnderProject("build/classes/kotlin/js/main").apply { mkdirs() }
        val marker = classpathDir.resolve("marker.txt").apply { writeText("v1") }

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

        val firstHash = runFingerprint()
        val firstManifest = fingerprintManifestFile().readText()
        assertManifestContainsDependencyIngredients(firstManifest, context = "baseline (directory classpath entry)")

        marker.writeText("v2")

        val secondHash = runFingerprint()
        val secondManifest = fingerprintManifestFile().readText()
        assertManifestContainsDependencyIngredients(secondManifest, context = "after directory content change")

        assertFingerprintChanged(firstHash, secondHash, "Fingerprint should change when a directory classpath entry contents change!")
        assertTrue(
            firstManifest != secondManifest,
            "Manifest should change when a directory classpath entry contents change.\n--- first ---\n$firstManifest\n--- second ---\n$secondManifest",
        )
    }

    @Test
    fun `aggregateFingerprints merges included + local manifest logs into one aggregate manifest log file`(@TempDir testProjectDir: File) {
        val includedDir = testProjectDir.resolve("my-included-lib").apply { mkdirs() }
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

        val mainDir = testProjectDir.resolve("main-app").apply { mkdirs() }
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

        gradle(mainDir, "aggregateFingerprints", "--no-configuration-cache")

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
    fun `aggregateFingerprints works when includedBuilds is not configured`() {
        writeSettings("aggregate-defaults-test")

        writeBuild(
            """
            plugins { id("com.zegreatrob.tools.fingerprint") }
            """,
        )

        val result = gradle(arguments = arrayOf("aggregateFingerprints", "--no-configuration-cache"))

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
    fun `fingerprint changes when published artifact bytes change even if sources and dependencies do not`() {
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

        fun buildWithManifest(implementationVersion: String) = """
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

        writeBuild(buildWithManifest("1"))
        val hash1 = runFingerprint()

        testProjectDir.resolve("build").deleteRecursively()

        writeBuild(buildWithManifest("2"))
        val hash2 = runFingerprint()

        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when the produced JAR bytes change (e.g., manifest attribute change), " +
                "even if sources and dependencies are unchanged.",
        )
    }

    @Test
    fun `fingerprint changes when KMP published artifact bytes change even if sources and dependencies do not`() {
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

        fun buildWithJvmJarManifest(implementationVersion: String) = """
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

        writeBuild(buildWithJvmJarManifest("1"))
        val hash1 = runFingerprint()

        testProjectDir.resolve("build").deleteRecursively()

        writeBuild(buildWithJvmJarManifest("2"))
        val hash2 = runFingerprint()

        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when a KMP published artifact (jvmJar) bytes change, even if sources/deps are unchanged.",
        )
    }

    @Test
    fun `fingerprint changes when build logic changes via buildSrc plugin implementation change`() {
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

        fun writeNoOpPluginSource(marker: String) {
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

        writeNoOpPluginSource(marker = "A")

        writeBuild(
            """
            plugins {
                java
                id("test.noop")
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }
            """,
        )

        val hash1 = runFingerprint()

        writeNoOpPluginSource(marker = "B")

        testProjectDir.resolve("build").deleteRecursively()

        val hash2 = runFingerprint()

        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when build logic (buildSrc plugin code) changes AND that change affects produced artifact bytes.",
        )
    }

    @Test
    fun `fingerprint changes when build logic changes via buildSrc convention plugin for KMP`() {
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

        fun writeConventionPluginSource(marker: String) {
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

        fun assertManifestShowsJvmJarWasFingerprinted(context: String) {
            val manifest = fingerprintManifestFile().readText()
            assertTrue(
                manifest.lineSequence().any { it.startsWith("artifact|") && it.contains("jvm", ignoreCase = true) && it.contains("jar", ignoreCase = true) },
                "Manifest must include an artifact line for the KMP JVM jar ($context). Manifest:\n$manifest",
            )
        }

        writeConventionPluginSource(marker = "A")

        writeBuild(
            """
            plugins {
                kotlin("multiplatform") version "2.3.0"
                id("test.kmpconvention")
                id("com.zegreatrob.tools.fingerprint")
            }

            repositories { mavenCentral() }

            kotlin { jvm() }
            """,
        )

        val hash1 = runFingerprint()
        assertManifestShowsJvmJarWasFingerprinted("first run")

        writeConventionPluginSource(marker = "B")
        testProjectDir.resolve("build").deleteRecursively()

        val hash2 = runFingerprint()
        assertManifestShowsJvmJarWasFingerprinted("second run")

        assertFingerprintChanged(
            hash1,
            hash2,
            "Fingerprint should change when KMP convention build logic changes AND that change affects published artifact bytes (jvmJar).",
        )
    }
}
