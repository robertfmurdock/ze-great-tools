package com.zegreatrob.tools.digger.json

import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

@Serializable
internal data class ContributionJson(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
    val dateTime: Instant? = null,
    val firstCommitDateTime: Instant? = null,
    val commitCount: Int,
    val ease: Int? = null,
    val storyId: String? = null,
    val semver: String? = null,
    val label: String? = null,
    val tagName: String? = null,
    val tagDateTime: Instant? = null,
)

fun Iterable<Contribution>.toJsonString(): String = Json.encodeToString(map(Contribution::toJsonModel))

fun Contribution.toJsonString(): String = Json.encodeToString(toJsonModel())

object ContributionParser {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parseContributions(jsonString: String) = json.decodeFromString<Array<ContributionJson>>(jsonString)
        .map(ContributionJson::toModel)

    fun parseContribution(jsonString: String) = json.decodeFromString<ContributionJson?>(jsonString)
        ?.toModel()
}

private fun Contribution.toJsonModel() = ContributionJson(
    lastCommit = lastCommit,
    firstCommit = firstCommit,
    authors = authors,
    dateTime = dateTime,
    firstCommitDateTime = firstCommitDateTime,
    ease = ease,
    storyId = storyId,
    semver = semver,
    label = label,
    tagName = tagName,
    tagDateTime = tagDateTime,
    commitCount = commitCount,
)

private fun ContributionJson.toModel() = Contribution(
    lastCommit = lastCommit,
    firstCommit = firstCommit,
    authors = authors,
    dateTime = dateTime,
    firstCommitDateTime = firstCommitDateTime,
    ease = ease,
    storyId = storyId,
    semver = semver,
    label = label,
    tagName = tagName,
    tagDateTime = tagDateTime,
    commitCount = commitCount,
)
