package com.zegreatrob.tools.plugins

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val testmintsBom = libs.findLibrary("testmints.bom")

val includeCommonMain = providers.provider {
    when (val propertyValue = findProperty("testmints.includeCommonMain")) {
        is Boolean -> propertyValue
        is String -> propertyValue.toBoolean()
        else -> false
    }
}
val dependencyHandler = dependencies
val bomProvider = testmintsBom.orElseThrow {
    error("Missing libs entry for com.zegreatrob.testmints.bom.")
}
val platformDependency = dependencyHandler.platform(bomProvider.get())
val configNames = setOf(
    "testImplementation",
    "commonTestImplementation",
    "jvmTestImplementation",
    "functionalTestImplementation",
)

configurations.configureEach {
    if (name in configNames || (name == "commonMainImplementation" && includeCommonMain.get())) {
        dependencyHandler.add(name, platformDependency)
    }
}
