package com.zegreatrob.tools.plugins

repositories {
    mavenCentral()
}

tasks {
    val projectResultPath =
        rootProject.layout.buildDirectory.dir(
            "test-output/${project.path}/results".replace(":", "/"),
        )

    val check = named("check")
    val copyReportsToRootDirectory by registering(Copy::class) {
        mustRunAfter(check)
        from("build/reports")
        into(projectResultPath)
    }
    val copyTestResultsToRootDirectory by registering(Copy::class) {
        mustRunAfter(check)
        from("build/test-results")
        into(projectResultPath)
    }
    register("collectResults") {
        group = "verification"
        description = "Collects test reports and results to root build directory"
        dependsOn(copyReportsToRootDirectory, copyTestResultsToRootDirectory)
    }
}

afterEvaluate {
    val testOutputPath = rootProject.layout.buildDirectory.dir("test-output")
    mkdir(testOutputPath.get().asFile)
}
