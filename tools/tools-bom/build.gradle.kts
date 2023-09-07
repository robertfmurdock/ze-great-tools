plugins {
    `java-platform`
    id("com.zegreatrob.tools.plugins.publish")
}

dependencies {
    constraints {
        api(project(":digger-core"))
        api(project(":digger-plugin"))
        api(project(":tagger-plugin"))
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
        }
    }
}
