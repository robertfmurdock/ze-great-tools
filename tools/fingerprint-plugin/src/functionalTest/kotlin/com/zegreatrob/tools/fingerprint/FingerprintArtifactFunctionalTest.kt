package com.zegreatrob.tools.fingerprint

import com.zegreatrob.testmints.setup
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class FingerprintArtifactFunctionalTest : FingerprintFunctionalTestBase() {

    @Test
    fun `fingerprint changes when published artifact bytes change even if sources and dependencies do not`() = setup(object {
        val initialVersion = "1"
        val updatedVersion = "2"
        val buildWithManifest: (String) -> String = { implementationVersion ->
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
    }) {
        writeSettings("published-artifact-change-test")

        writeProjectFile(
            "src/main/java/example/Hello.java",
            """
            package example;

            public class Hello {
                public static String value() { return "hello"; }
            }
            """,
        )
    } exercise {
        writeBuild(buildWithManifest(initialVersion))
        val hash1 = runFingerprint()

        testProjectDir.resolve("build").deleteRecursively()

        writeBuild(buildWithManifest(updatedVersion))
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
        val initialVersion = "1"
        val updatedVersion = "2"
        val buildWithJvmJarManifest: (String) -> String = { implementationVersion ->
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
    } exercise {
        writeBuild(buildWithJvmJarManifest(initialVersion))
        val hash1 = runFingerprint()

        testProjectDir.resolve("build").deleteRecursively()

        writeBuild(buildWithJvmJarManifest(updatedVersion))
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
        val initialMarker = "A"
        val updatedMarker = "B"
        val writeNoOpPluginSource: (String) -> Unit = { marker ->
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

        writeNoOpPluginSource(initialMarker)

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

        writeNoOpPluginSource(updatedMarker)

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
        val initialMarker = "A"
        val updatedMarker = "B"
        val writeConventionPluginSource: (String) -> Unit = { marker ->
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

        writeConventionPluginSource(initialMarker)

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

        writeConventionPluginSource(updatedMarker)
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
}
