package com.zegreatrob.tools.tagger

class CalculateVersionConfigFileFunctionalTest : CalculateVersionTestSpec {
    override lateinit var projectDir: String

    override val addFileNames: Set<String>
        get() = ConfigFileFunctionalTestSupport.addFileNames()

    override fun configureWithDefaults() {
        ConfigFileFunctionalTestSupport.setupConfigFileBuild(projectDir)
        ConfigFileFunctionalTestSupport.writeTaggerFile(projectDir, listOf("\"releaseBranch\": \"master\""))
    }

    override fun configureWithOverrides(
        implicitPatch: Boolean?,
        disableDetached: Boolean?,
        allowDetachedHead: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
        forceSnapshot: Boolean?,
        warningsAsErrors: Boolean?,
    ) {
        ConfigFileFunctionalTestSupport.setupConfigFileBuild(projectDir)
        ConfigFileFunctionalTestSupport.writeTaggerFile(
            projectDir,
            listOfNotNull(
                "\"releaseBranch\": \"master\"",
                ConfigFileFunctionalTestSupport.bool("implicitPatch", implicitPatch),
                ConfigFileFunctionalTestSupport.bool("disableDetached", disableDetached),
                ConfigFileFunctionalTestSupport.bool("allowDetachedHead", allowDetachedHead),
                ConfigFileFunctionalTestSupport.escaped("majorRegex", majorRegex),
                ConfigFileFunctionalTestSupport.escaped("minorRegex", minorRegex),
                ConfigFileFunctionalTestSupport.escaped("patchRegex", patchRegex),
                ConfigFileFunctionalTestSupport.escaped("versionRegex", versionRegex),
                ConfigFileFunctionalTestSupport.escaped("noneRegex", noneRegex),
                ConfigFileFunctionalTestSupport.bool("forceSnapshot", forceSnapshot),
                ConfigFileFunctionalTestSupport.bool("warningsAsErrors", warningsAsErrors),
            ),
        )
    }

    override fun execute(): TestResult {
        val output = ConfigFileFunctionalTestSupport.gradleOutput(projectDir, "calculateVersion", "-q")
        return output.fold(
            onSuccess = ConfigFileFunctionalTestSupport::parseCalculateVersion,
            onFailure = { TestResult.Failure(it.message!!) },
        )
    }
}
