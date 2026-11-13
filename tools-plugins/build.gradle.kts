
import nl.littlerobots.vcu.plugin.versionSelector
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("com.zegreatrob.tools.fingerprint") version "1.8.74"
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    alias(libs.plugins.org.jmailen.kotlinter)
}

group = "com.zegreatrob.tools"
subprojects {
    group = rootProject.group
}

fingerprintConfig {
    includedProjects = setOf("dashboard-test-summary")
}

gradlePlugin {
    plugins {
        register("npm-cli") {
            id = "com.zegreatrob.tools.plugins.npm-cli"
            implementationClass = "com.zegreatrob.tools.plugins.NpmCliPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", embeddedKotlinVersion))
    implementation(libs.org.jmailen.gradle.kotlinter.gradle)
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$embeddedKotlinVersion")
    }
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5"))
    testImplementation("com.zegreatrob.testmints:standard:12.7.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

tasks {
    test { useJUnitPlatform() }
    register("release") {
        group = "publishing"
        description = "Publishes every Gradle plugin in this build to the Gradle Plugin Portal"
        if (!project.version.toString().contains("SNAPSHOT")) {
            dependsOn(provider { getTasksByName("publishPlugins", true) })
        }
    }
    assemble { dependsOn(provider { (getTasksByName("assemble", true) - this).toList() }) }
    clean { dependsOn(provider { (getTasksByName("clean", true) - this).toList() }) }
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<KotlinCompile> {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}

versionCatalogUpdate {
    val rejectRegex = "^[0-9.]+[0-9](-RC|-M[0-9]*|-RC[0-9]*.*|-beta.*|-Beta.*|-alpha.*|-dev.*|.*-compat.*|-rc[0-9]*)$".toRegex()
    versionSelector { versionCandidate ->
        !rejectRegex.matches(versionCandidate.candidate.version)
    }
}
