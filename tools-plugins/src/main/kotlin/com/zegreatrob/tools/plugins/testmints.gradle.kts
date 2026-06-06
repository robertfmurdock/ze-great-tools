package com.zegreatrob.tools.plugins

import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

afterEvaluate {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
    val includeCommonMain = resolveIncludeCommonMain()
    val configNames = buildConfigurationNames(includeCommonMain)
    applyTestmintsBom(libs, configNames)
}

fun Project.resolveIncludeCommonMain(): Boolean = when (val propertyValue = findProperty("testmints.includeCommonMain")) {
    is Boolean -> propertyValue
    is String -> propertyValue.toBoolean()
    else -> false
}

fun buildConfigurationNames(includeCommonMain: Boolean): List<String> {
    val baseConfigs = listOf(
        "testImplementation",
        "commonTestImplementation",
        "jvmTestImplementation",
        "functionalTestImplementation",
    )
    return if (includeCommonMain) baseConfigs + "commonMainImplementation" else baseConfigs
}

fun Project.applyTestmintsBom(libs: VersionCatalog, configNames: List<String>) {
    val bomProvider = libs.findLibrary("com.zegreatrob.testmints.bom")
        .orElseThrow { error("Missing libs entry for com.zegreatrob.testmints.bom.") }
    configNames.forEach { configName ->
        configurations.findByName(configName)?.let {
            dependencies.add(configName, dependencies.platform(bomProvider))
        }
    }
}
