package com.zegreatrob.tools.adapter.git

import kotlinx.datetime.Instant

data class TagRef(val name: String, val commitId: String, val dateTime: Instant)
