package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.tag

class Tag : CliktCommand() {
    private val dir by argument("git-repo")
    private val releaseBranch by option().required()
    private val version: String by option().required()
    override fun run() {
        TaggerCore(GitAdapter(dir))
            .tag(version, releaseBranch)
    }
}
