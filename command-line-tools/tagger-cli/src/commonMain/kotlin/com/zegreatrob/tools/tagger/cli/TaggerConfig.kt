package com.zegreatrob.tools.tagger.cli

import kotlinx.serialization.Serializable

@Serializable
data class TaggerConfig(
    val releaseBranch: String? = null,
    val implicitPatch: Boolean? = null,
    val disableDetached: Boolean? = null,
    val majorRegex: String? = null,
    val minorRegex: String? = null,
    val patchRegex: String? = null,
    val versionRegex: String? = null,
    val noneRegex: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val warningsAsErrors: Boolean? = null,
)
