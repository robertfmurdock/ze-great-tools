package com.zegreatrob.tools.tagger.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.zegreatrob.tools.adapter.git.GitAdapter
import com.zegreatrob.tools.tagger.core.TaggerCore
import com.zegreatrob.tools.tagger.core.VersionRegex
import com.zegreatrob.tools.tagger.core.calculateNextVersion

class CalculateVersion : CliktCommand() {

    init {
        context { valueSources(ConfigFileSource(envvarReader)) }
    }

    private val dir by argument("git-repo")
    private val implicitPatch by option().boolean().default(true)
    private val releaseBranch by option()
    private val majorRegex by option().default(VersionRegex.Defaults.major.pattern)
    private val minorRegex by option().default(VersionRegex.Defaults.minor.pattern)
    private val patchRegex by option().default(VersionRegex.Defaults.patch.pattern)
    private val noneRegex by option().default(VersionRegex.Defaults.none.pattern)
    private val versionRegex by option().check(
        message = VersionRegex.MISSING_GROUP_ERROR,
        validator = VersionRegex.Companion::containsAllGroups,
    )

    override fun run() {
        TaggerCore(GitAdapter(dir))
            .calculateNextVersion(
                implicitPatch = implicitPatch,
                versionRegex = VersionRegex(
                    none = Regex(noneRegex, RegexOption.IGNORE_CASE),
                    patch = Regex(patchRegex, RegexOption.IGNORE_CASE),
                    minor = Regex(minorRegex, RegexOption.IGNORE_CASE),
                    major = Regex(majorRegex, RegexOption.IGNORE_CASE),
                    unified = versionRegex?.let { Regex(it, RegexOption.IGNORE_CASE) },
                ),
                releaseBranch = releaseBranch ?: "",
            ).let(::echo)
    }
}
