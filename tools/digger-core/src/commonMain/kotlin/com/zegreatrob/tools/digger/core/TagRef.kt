package com.zegreatrob.tools.digger.core

import kotlinx.datetime.Instant

data class TagRef(val name: String, val commitId: String, val dateTime: Instant)
