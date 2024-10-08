package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TagResult
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.tag

class Tag : CliktCommand() {

    init {
        context { valueSources(ConfigFileSource(readEnvvar)) }
    }

    private val gitRepoArgument by argument("git-repo").optional()
    private val gitRepoOption by option("--git-repo", envvar = "PWD")
    private val workingDirectory get() = gitRepoArgument ?: gitRepoOption ?: throw CliktError("No target directory")
    private val releaseBranch by option().required()
    private val version: String by option().required()
    private val userName: String? by option()
    private val userEmail: String? by option()
    private val warningsAsErrors by option().boolean().default(false)
    override fun run() {
        TaggerCore(GitAdapter(workingDirectory))
            .tag(version, releaseBranch, userName, userEmail)
            .let {
                when (it) {
                    TagResult.Success -> echo("Success!")
                    is TagResult.Error -> if (warningsAsErrors) {
                        throw CliktError(it.message)
                    } else {
                        echo(it.message, err = true)
                    }
                }
            }
    }
}
