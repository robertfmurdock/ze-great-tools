plugins {
    `java-platform`
    id("com.zegreatrob.tools.plugins.publish")
}

dependencies {
    constraints {
        api(project(":tagger-plugin"))
        api(project(":digger-plugin"))
        api(project(":digger"))
    }
}

publishing {
    publications {
        create<MavenPublication>("bom") {
            from(components["javaPlatform"])
        }
    }
}
