plugins {
    `java-platform`
    id("com.zegreatrob.tools.plugins.publish")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
}

publishing {
    publications {
        val bomPublication = create<MavenPublication>("bom") {
            from(components["javaPlatform"])
        }
        tasks.withType<AbstractPublishToMaven> {
            enabled = this@withType.publication == bomPublication
        }
    }
}
