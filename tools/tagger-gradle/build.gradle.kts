plugins {
    id("com.zegreatrob.tools.plugins.jvm")
    `java-library`
}

dependencies {
    api(project(":tagger-core"))
    api(project(":git-adapter"))
}
