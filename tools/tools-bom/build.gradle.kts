plugins {
    `java-platform`
    id("com.zegreatrob.tools.plugins.publish")
}

dependencies {
    constraints {
        api(project(":certifier-plugin"))
        api(project(":digger-core"))
        api(project(":digger-json"))
        api(project(":digger-model"))
        api(project(":digger-plugin"))
        api(project(":fingerprint-plugin"))
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
