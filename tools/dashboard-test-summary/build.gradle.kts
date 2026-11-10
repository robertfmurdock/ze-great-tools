plugins {
    id("com.zegreatrob.tools.plugins.jvm")
    `java-gradle-plugin`
    alias(libs.plugins.com.gradle.plugin.publish)
    id("com.zegreatrob.tools.plugins.publish")
}

dependencies {
    implementation(platform(libs.org.jetbrains.kotlinx.kotlinx.serialization.bom))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5", embeddedKotlinVersion))
    testImplementation(libs.com.zegreatrob.testmints.minassert)
    testImplementation(libs.com.zegreatrob.testmints.standard)
    testRuntimeOnly(platform(libs.org.junit.junit.bom))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
