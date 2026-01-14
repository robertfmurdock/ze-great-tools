
import nl.littlerobots.vcu.plugin.versionSelector
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
    alias(libs.plugins.nl.littlerobots.version.catalog.update)
    alias(libs.plugins.org.jmailen.kotlinter)
}

dependencies {
    implementation(kotlin("stdlib", embeddedKotlinVersion))
    implementation(kotlin("gradle-plugin", embeddedKotlinVersion))
    implementation(libs.org.jmailen.gradle.kotlinter.gradle)
}

tasks {
    assemble { dependsOn(provider { (getTasksByName("assemble", true) - this).toList() }) }
    clean { dependsOn(provider { (getTasksByName("clean", true) - this).toList() }) }
    withType<FormatTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
    withType<LintTask> {
        exclude { spec -> spec.file.absolutePath.contains("generated-sources") }
    }
}

versionCatalogUpdate {
    val rejectRegex = "^[0-9.]+[0-9](-RC|-M[0-9]*|-RC[0-9]*.*|-beta.*|-Beta.*|-alpha.*|-dev.*)$".toRegex()
    versionSelector { versionCandidate ->
        !rejectRegex.matches(versionCandidate.candidate.version)
    }
}
