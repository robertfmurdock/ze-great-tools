package com.zegreatrob.tools.tagger.json

import com.zegreatrob.tools.tagger.core.VersionRegex
import kotlinx.serialization.Serializable

@Serializable
data class TaggerConfig(
    val releaseBranch: String? = null,
    val implicitPatch: Boolean? = null,
    val disableDetached: Boolean? = null,
    val majorRegex: String? = null,
    val minorRegex: String? = null,
    val patchRegex: String? = null,
    val noneRegex: String? = null,
    val versionRegex: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val warningsAsErrors: Boolean? = null,
)

val runtimeDefaultConfig = TaggerConfig(
    implicitPatch = true,
    disableDetached = true,
    majorRegex = VersionRegex.Defaults.major.pattern,
    minorRegex = VersionRegex.Defaults.minor.pattern,
    patchRegex = VersionRegex.Defaults.patch.pattern,
    noneRegex = VersionRegex.Defaults.none.pattern,
    warningsAsErrors = false,
)
