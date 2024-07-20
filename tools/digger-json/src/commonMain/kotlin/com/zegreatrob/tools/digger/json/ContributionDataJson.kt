package com.zegreatrob.tools.digger.json

import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class ContributionJson(
    val lastCommit: String,
    val firstCommit: String,
    val authors: List<String>,
    val dateTime: Instant? = null,
    val firstCommitDateTime: Instant? = null,
    val ease: Int? = null,
    val storyId: String? = null,
    val semver: String? = null,
    val label: String? = null,
)

fun Iterable<Contribution>.toJsonString(): String = Json.encodeToString(map(Contribution::toJsonModel))

fun Contribution.toJsonString(): String = Json.encodeToString(toJsonModel())

object ContributionParser {
    fun parseContributions(jsonString: String) =
        Json.decodeFromString<Array<ContributionJson>>(jsonString)
            .map(ContributionJson::toModel)

    fun parseContribution(jsonString: String) =
        Json.decodeFromString<ContributionJson?>(jsonString)
            ?.toModel()
}

private fun Contribution.toJsonModel() =
    ContributionJson(
        lastCommit = lastCommit,
        dateTime = dateTime,
        firstCommit = firstCommit,
        firstCommitDateTime = firstCommitDateTime,
        authors = authors,
        ease = ease,
        storyId = storyId,
        semver = semver,
        label = label,
    )

private fun ContributionJson.toModel() =
    Contribution(
        lastCommit = lastCommit,
        dateTime = dateTime,
        firstCommit = firstCommit,
        firstCommitDateTime = firstCommitDateTime,
        authors = authors,
        ease = ease,
        storyId = storyId,
        semver = semver,
        label = label,
    )
