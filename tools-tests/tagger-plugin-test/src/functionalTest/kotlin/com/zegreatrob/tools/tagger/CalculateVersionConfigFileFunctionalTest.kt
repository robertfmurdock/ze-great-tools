package com.zegreatrob.tools.tagger

class CalculateVersionConfigFileFunctionalTest :
    CalculateVersionTestSpec,
    CalculateVersionConfigFileParseFailureTestSpec {
    override lateinit var projectDir: String

    override val addFileNames: Set<String>
        get() = ConfigFileFunctionalTestSupport.addFileNames()

    private var enableTransparency: Boolean = false

    override fun configureWithDefaults() {
        enableTransparency = false
        ConfigFileFunctionalTestSupport.setupConfigFileBuild(projectDir)
        ConfigFileFunctionalTestSupport.writeTaggerFile(projectDir, listOf("\"releaseBranch\": \"master\""))
    }

    override fun configureWithRawTaggerConfig(contents: String) {
        ConfigFileFunctionalTestSupport.setupConfigFileBuild(projectDir)
        ConfigFileFunctionalTestSupport.writeRawTaggerFile(projectDir, contents)
    }

    override fun configureWithOverrides(
        implicitPatch: Boolean?,
        allowDetachedHead: Boolean?,
        majorRegex: String?,
        minorRegex: String?,
        patchRegex: String?,
        versionRegex: String?,
        noneRegex: String?,
        forceSnapshot: Boolean?,
        warningsAsErrors: Boolean?,
        showCommands: Boolean?,
    ) {
        enableTransparency = showCommands == true
        ConfigFileFunctionalTestSupport.setupConfigFileBuild(projectDir)
        ConfigFileFunctionalTestSupport.writeTaggerFile(
            projectDir,
            listOfNotNull(
                "\"releaseBranch\": \"master\"",
                ConfigFileFunctionalTestSupport.bool("implicitPatch", implicitPatch),
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
        val args = if (enableTransparency) arrayOf("calculateVersion") else arrayOf("calculateVersion", "-q")
        val output = ConfigFileFunctionalTestSupport.gradleOutput(projectDir, *args)
        return output.fold(
            onSuccess = ConfigFileFunctionalTestSupport::parseCalculateVersion,
            onFailure = { TestResult.Failure(it.message!!) },
        )
    }
}
