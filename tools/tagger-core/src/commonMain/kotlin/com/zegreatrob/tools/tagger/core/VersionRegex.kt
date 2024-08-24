package com.zegreatrob.tools.tagger.core

import com.zegreatrob.tools.digger.core.MessageDigger

data class VersionRegex(
    val none: Regex,
    val patch: Regex,
    val minor: Regex,
    val major: Regex,
    val unified: Regex?,
) {
    object Defaults {
        val none = MessageDigger.Defaults.noneRegex
        val patch = MessageDigger.Defaults.patchRegex
        val minor = MessageDigger.Defaults.minorRegex
        val major = MessageDigger.Defaults.majorRegex
    }

    companion object {

        fun containsAllGroups(regexPattern: String) = regexPattern.contains("?<major>") &&
            regexPattern.contains("?<minor>") &&
            regexPattern.contains("?<patch>") &&
            regexPattern.contains("?<none>")

        const val MISSING_GROUP_ERROR = "version regex must include groups named 'major', 'minor', 'patch', and 'none'."
    }
}
