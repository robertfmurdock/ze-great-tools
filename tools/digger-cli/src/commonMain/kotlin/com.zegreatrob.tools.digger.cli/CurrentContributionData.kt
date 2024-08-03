package com.zegreatrob.tools.digger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.zegreatrob.tools.digger.core.DiggerGitWrapper
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.core.TagRef
import com.zegreatrob.tools.digger.core.contribution
import com.zegreatrob.tools.digger.core.currentCommitTag
import com.zegreatrob.tools.digger.core.currentContributionCommits
import com.zegreatrob.tools.digger.json.toJsonString
import com.zegreatrob.tools.digger.model.Contribution

class CurrentContributionData : CliktCommand() {
    private val dir by option().default("")

    private val gitDigger get() = DiggerGitWrapper(dir)
    private val messageDigger = MessageDigger()

    private fun currentContributionData() =
        with(gitDigger) {
            messageDigger.contribution(currentContributionCommits())
                .copyWithLabelAndTag(currentCommitTag())
        }

    private fun Contribution.copyWithLabelAndTag(currentCommitTag: TagRef?) = copy(
        label = "fix-me",
        tagName = currentCommitTag?.name,
        tagDateTime = currentCommitTag?.dateTime,
    )

    override fun run() {
        echo(currentContributionData().toJsonString())
    }
}
