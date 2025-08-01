
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

repositories {
    maven { url = uri("https://plugins.gradle.org/m2/") }
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    alias(libs.plugins.com.github.ben.manes.versions)
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    alias(libs.plugins.org.jmailen.kotlinter)
}

dependencies {
    implementation(kotlin("stdlib", embeddedKotlinVersion))
    implementation(kotlin("gradle-plugin", embeddedKotlinVersion))
    implementation(libs.com.github.ben.manes.gradle.versions.plugin)
    implementation(libs.org.jmailen.gradle.kotlinter.gradle)
}

tasks {
    withType<DependencyUpdatesTask> {
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
        revision = "release"

        rejectVersionIf {
            "^[0-9.]+[0-9](-RC|-M[0-9]+|-RC[0-9]+|-beta.*|-Beta.*|-alpha.*)$"
                .toRegex(RegexOption.IGNORE_CASE)
                .matches(candidate.version)
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
}
