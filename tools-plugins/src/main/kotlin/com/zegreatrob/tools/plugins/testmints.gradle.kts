package com.zegreatrob.tools.plugins

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val testmintsBom = libs.findLibrary("com.zegreatrob.testmints.bom")

afterEvaluate {
    val includeCommonMain = when (val propertyValue = findProperty("testmints.includeCommonMain")) {
        is Boolean -> propertyValue
        is String -> propertyValue.toBoolean()
        else -> false
    }
    val bomProvider = testmintsBom.orElseThrow {
        error("Missing libs entry for com.zegreatrob.testmints.bom.")
    }
    val configNames = mutableListOf(
        "testImplementation",
        "commonTestImplementation",
        "jvmTestImplementation",
        "functionalTestImplementation",
    )
    if (includeCommonMain) {
        configNames.add("commonMainImplementation")
    }
    configNames.forEach { configName ->
        configurations.findByName(configName)?.let {
            val platformDependency = dependencies.platform(bomProvider)
            dependencies.add(configName, platformDependency)
        }
    }
}
