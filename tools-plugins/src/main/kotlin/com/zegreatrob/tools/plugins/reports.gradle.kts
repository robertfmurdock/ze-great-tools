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
    val copyReportsToRootDirectory = register<Copy>("copyReportsToRootDirectory") {
        group = "verification"
        description = "Copies reports to the root test output directory"
        mustRunAfter(check)
        from(layout.buildDirectory.dir("reports"))
        into(projectResultPath.map { it.dir("reports") })
    }
    val copyTestResultsToRootDirectory = register<Copy>("copyTestResultsToRootDirectory") {
        group = "verification"
        description = "Copies test results to the root test output directory"
        mustRunAfter(check)
        from(layout.buildDirectory.dir("test-results"))
        into(projectResultPath.map { it.dir("test-results") })
    }
    register("collectResults") {
        group = "verification"
        description = "Collects reports and test results in the root build directory"
        dependsOn(copyReportsToRootDirectory, copyTestResultsToRootDirectory)
    }
}
