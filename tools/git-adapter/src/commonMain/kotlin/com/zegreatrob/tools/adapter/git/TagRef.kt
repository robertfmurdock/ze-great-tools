package com.zegreatrob.tools.adapter.git

import kotlin.time.Instant

data class TagRef(val name: String, val commitId: String, val dateTime: Instant)
