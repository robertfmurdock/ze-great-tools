package com.zegreatrob.tools.plugins

repositories {
    mavenCentral()
}

tasks {
    val projectResultPath =
        rootProject.layout.buildDirectory.dir(
            "test-output/${project.path}/results".replace(":", "/"),
        )

    val check = getByName("check")
    val copyReportsToRootDirectory = register<CopyReportFiles>("copyReportsToRootDirectory") {
        mustRunAfter(check)
        sourceDirectory.set(layout.buildDirectory.dir("reports"))
        sourceFiles.from(sourceDirectory.asFileTree)
        destinationDirectory.set(projectResultPath)
    }
    val copyTestResultsToRootDirectory = register<CopyReportFiles>("copyTestResultsToRootDirectory") {
        mustRunAfter(check)
        sourceDirectory.set(layout.buildDirectory.dir("test-results"))
        sourceFiles.from(sourceDirectory.asFileTree)
        destinationDirectory.set(projectResultPath)
    }
    register("collectResults") {
        dependsOn(copyReportsToRootDirectory, copyTestResultsToRootDirectory)
    }
}
