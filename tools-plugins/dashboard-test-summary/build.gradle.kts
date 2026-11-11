plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.com.gradle.plugin.publish)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5", embeddedKotlinVersion))
    testImplementation("com.zegreatrob.testmints:minassert:12.6.70")
    testImplementation("com.zegreatrob.testmints:standard:12.6.70")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

gradlePlugin {
    website.set("https://github.com/robertfmurdock/ze-great-tools")
    vcsUrl.set("https://github.com/robertfmurdock/ze-great-tools")
    plugins {
        create("dashboardTestSummary") {
            id = "com.zegreatrob.tools.dashboard-test-summary"
            implementationClass = "com.zegreatrob.tools.dashboard.DashboardTestSummaryPlugin"
            displayName = "Dashboard Test Summary"
            description = "Aggregates JUnit XML into a Ze Great Dashboard test summary."
            tags.addAll("junit", "testing", "reporting", "composite-build")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
