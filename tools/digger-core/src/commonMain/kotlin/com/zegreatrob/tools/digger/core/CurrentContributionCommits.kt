package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.wrapper.git.CommitRef
import com.zegreatrob.tools.wrapper.git.GitAdapter
import com.zegreatrob.tools.wrapper.git.TagRef

fun GitAdapter.currentContributionCommits(previousTag: TagRef?): List<CommitRef> = if (previousTag == null) {
    log()
} else {
    logWithRange("^${previousTag.name}", "HEAD")
}
