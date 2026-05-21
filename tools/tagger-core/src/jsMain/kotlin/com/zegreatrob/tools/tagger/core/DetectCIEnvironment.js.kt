package com.zegreatrob.tools.tagger.core

@JsModule("process")
@JsNonModule
external object Process {
    val env: dynamic
}

actual fun detectCIEnvironment(): CIEnvironment {
    val env = Process.env
    return when {
        env.GITHUB_ACTIONS != undefined -> CIEnvironment.GITHUB_ACTIONS
        env.GITLAB_CI != undefined -> CIEnvironment.GITLAB_CI
        env.TF_BUILD != undefined || env.AZURE_PIPELINE != undefined -> CIEnvironment.AZURE_DEVOPS
        else -> CIEnvironment.UNKNOWN
    }
}
