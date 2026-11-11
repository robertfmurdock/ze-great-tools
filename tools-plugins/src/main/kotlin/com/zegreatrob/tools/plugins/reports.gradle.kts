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
        group = "verification"
        description = "Copies reports to the root test output directory"
        mustRunAfter(check)
        sourceDirectory.set(layout.buildDirectory.dir("reports"))
        sourceFiles.from(sourceDirectory.asFileTree)
        destinationDirectory.set(projectResultPath)
    }
    val copyTestResultsToRootDirectory = register<CopyReportFiles>("copyTestResultsToRootDirectory") {
        group = "verification"
        description = "Copies test results to the root test output directory"
        mustRunAfter(check)
        sourceDirectory.set(layout.buildDirectory.dir("test-results"))
        sourceFiles.from(sourceDirectory.asFileTree)
        destinationDirectory.set(projectResultPath)
    }
    register("collectResults") {
        group = "verification"
        description = "Collects reports and test results in the root build directory"
        dependsOn(copyReportsToRootDirectory, copyTestResultsToRootDirectory)
    }
}
