package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.json.ContributionParser

fun parseCurrentAuthors(output: String) = ContributionParser.parseContribution(output)?.authors
fun parseSemver(output: String) = ContributionParser.parseContribution(output)?.semver
fun parseStoryId(output: String) = ContributionParser.parseContribution(output)?.storyId
fun parseAll(output: String) = ContributionParser.parseContributions(output)
