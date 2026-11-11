dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal {
            content { includeGroup("org.jmailen.gradle") }
        }
    }
}

rootProject.name = "tools-plugins"
include("dashboard-test-summary")
