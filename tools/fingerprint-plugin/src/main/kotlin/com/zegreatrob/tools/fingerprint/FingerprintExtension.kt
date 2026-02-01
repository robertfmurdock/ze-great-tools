package com.zegreatrob.tools.fingerprint

import org.gradle.api.provider.SetProperty

interface FingerprintExtension {
    val includedProjects: SetProperty<String>
    val includedBuilds: SetProperty<String>
}
