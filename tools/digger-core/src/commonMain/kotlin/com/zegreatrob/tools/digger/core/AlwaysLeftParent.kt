package com.zegreatrob.tools.digger.core

import com.zegreatrob.tools.adapter.git.CommitRef

fun List<CommitRef>.alwaysLeftParent() = fold(emptyList<CommitRef>()) { acc, commit ->
    if (acc.isEmpty()) {
        acc + commit
    } else if (commit.id == acc.last().parents.firstOrNull()) {
        acc + commit
    } else {
        acc
    }
}
