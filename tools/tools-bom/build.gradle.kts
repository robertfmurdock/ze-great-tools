plugins {
    `java-platform`
    id("com.zegreatrob.tools.plugins.publish")
}

dependencies {
    constraints {
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
