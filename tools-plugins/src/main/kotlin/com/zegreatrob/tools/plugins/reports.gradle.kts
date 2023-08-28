package com.zegreatrob.tools.plugins

repositories {
    mavenCentral()
}

tasks {
    val projectResultPath = rootProject.layout.buildDirectory.dir(
        "test-output/${project.path}/results".replace(":", "/"),
    )

    val check by getting
    val copyReportsToRootDirectory by creating(Copy::class) {
        mustRunAfter(check)
        from("build/reports")
        into(projectResultPath)
    }
    val copyTestResultsToRootDirectory by creating(Copy::class) {
        mustRunAfter(check)
        from("build/test-results")
        into(projectResultPath)
    }
    register("collectResults") {
        dependsOn(copyReportsToRootDirectory, copyTestResultsToRootDirectory)
    }
}

afterEvaluate {
    val testOutputPath = rootProject.layout.buildDirectory.dir("test-output")
    mkdir(testOutputPath.get().asFile)
}
