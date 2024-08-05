package com.zegreatrob.tools.digger

interface SetupWithOverrides {
    fun setupWithOverrides(
        label: String? = null,
        majorRegex: String? = null,
        minorRegex: String? = null,
        patchRegex: String? = null,
        noneRegex: String? = null,
    )
}
