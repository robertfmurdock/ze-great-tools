plugins {
    id("com.zegreatrob.tools.tagger")
    base
}

tagger {
    releaseBranch = "main"
}

tasks {
    check {
        dependsOn(provider {gradle.includedBuilds.map { it.task(":check") }.toList()})
    }
}
