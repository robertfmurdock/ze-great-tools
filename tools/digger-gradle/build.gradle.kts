plugins {
    id("com.zegreatrob.tools.plugins.jvm")
    `java-library`
}

dependencies {
    api(project(":digger-core"))
    api(project(":digger-json"))
    api(project(":git-adapter"))
}
