package com.zegreatrob.tools.tagger.core

actual fun detectCIEnvironment(): CIEnvironment {
    val env = System.getenv()
    return when {
        env.containsKey("GITHUB_ACTIONS") -> CIEnvironment.GITHUB_ACTIONS
        env.containsKey("GITLAB_CI") -> CIEnvironment.GITLAB_CI
        env.containsKey("TF_BUILD") || env.containsKey("AZURE_PIPELINE") -> CIEnvironment.AZURE_DEVOPS
        else -> CIEnvironment.UNKNOWN
    }
}
