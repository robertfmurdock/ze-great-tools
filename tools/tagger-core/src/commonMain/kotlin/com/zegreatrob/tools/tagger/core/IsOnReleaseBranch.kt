package com.zegreatrob.tools.tagger.core

fun TaggerCore.isOnReleaseBranch(
    releaseBranch: String?,
) = adapter.status().head == releaseBranch
