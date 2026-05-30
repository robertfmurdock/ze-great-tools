package com.zegreatrob.tools.tagger

class TagConfigFileFunctionalTest : TagTestSpec {

    override lateinit var projectDir: String

    override val addFileNames: Set<String>
        get() = ConfigFileFunctionalTestSupport.addFileNames()

    override fun configureWithDefaults() {
        ConfigFileFunctionalTestSupport.setupConfigFileBuild(projectDir)
        ConfigFileFunctionalTestSupport.writeTaggerFile(projectDir, listOf("\"releaseBranch\": \"master\""))
    }

    override fun configureWithOverrides(
        releaseBranch: String?,
        userName: String?,
        userEmail: String?,
        warningsAsErrors: Boolean?,
    ) {
        ConfigFileFunctionalTestSupport.setupConfigFileBuild(projectDir)
        ConfigFileFunctionalTestSupport.writeTaggerFile(
            projectDir,
            listOfNotNull(
                ConfigFileFunctionalTestSupport.quoted("releaseBranch", releaseBranch),
                ConfigFileFunctionalTestSupport.quoted("userName", userName),
                ConfigFileFunctionalTestSupport.quoted("userEmail", userEmail),
                ConfigFileFunctionalTestSupport.bool("warningsAsErrors", warningsAsErrors),
            ),
        )
    }

    override fun execute(version: String): TestResult {
        val output = ConfigFileFunctionalTestSupport.gradleOutput(projectDir, "tag", "-Pversion=$version")
        return output.fold(
            onSuccess = { TestResult.Success(it.trim()) },
            onFailure = { TestResult.Failure(it.message!!.trim()) },
        )
    }
}
