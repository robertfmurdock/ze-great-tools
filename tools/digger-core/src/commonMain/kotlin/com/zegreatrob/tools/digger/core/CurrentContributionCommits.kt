package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.adapter.git.CommitRef
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.adapter.git.TagRef

fun GitAdapter.currentContributionCommits(previousTag: TagRef?): List<CommitRef> = if (previousTag == null) {
    log()
} else {
    logWithRange("^${previousTag.name}", "HEAD")
}
