@file:OptIn(ExperimentalUuidApi::class)

package com.zegreatrob.tools.digger.json

import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ContributionParserTest {
    @Test
    fun canRoundTripSuccessfully() {
        val contribution = stubContribution()
        val loaded = ContributionParser.parseContribution(contribution.toJsonString())
        assertEquals(contribution, loaded)
    }

    @Test
    fun willIgnoreExtraAttributes() {
        val contribution = stubContribution()
        val jsonString = contribution.toJsonString()

        val jsonElement = Json.parseToJsonElement(jsonString)

        val augmentedJsonElement = buildJsonObject {
            jsonElement.jsonObject.forEach {
                put(it.key, it.value)
            }
            put("extraExtra", "readAllAboutIt")
        }

        val loaded = ContributionParser.parseContribution(augmentedJsonElement.toString())
        assertEquals(contribution, loaded)
    }

    @Test
    fun willTolerateMissingAttributes() {
        val contribution = stubContribution()
        val jsonString = contribution.toJsonString()

        val jsonElement = Json.parseToJsonElement(jsonString)

        val notMandatoryAttributes = listOf(
            "dateTime",
            "firstCommitDateTime",
            "ease",
            "storyId",
            "semver",
            "label",
        )

        val augmentedJsonElement = buildJsonObject {
            jsonElement.jsonObject.forEach {
                if (it.key !in notMandatoryAttributes) {
                    put(it.key, it.value)
                }
            }
        }

        val loaded = ContributionParser.parseContribution(augmentedJsonElement.toString())
        assertEquals(
            contribution.copy(
                dateTime = null,
                firstCommitDateTime = null,
                ease = null,
                storyId = null,
                semver = null,
                label = null,
            ),
            loaded,
        )
    }

    @Test
    fun canRoundTripListSuccessfully() {
        val contributions = listOf(stubContribution(), stubContribution(), stubContribution())
        val loaded = ContributionParser.parseContributions(contributions.toJsonString())
        assertEquals(contributions, loaded)
    }

    private fun stubContribution() = Contribution(
        lastCommit = "${Uuid.random()}",
        firstCommit = "${Uuid.random()}",
        authors = listOf("${Uuid.random()}"),
        dateTime =
        randomInstant(),
        firstCommitDateTime =
        randomInstant(),
        ease = Random.nextInt(),
        storyId = "${Uuid.random()}",
        semver = "${Uuid.random()}",
        label = "${Uuid.random()}",
        tagDateTime = randomInstant(),
        tagName = "${Uuid.random()}",
        commitCount = Random.nextInt(10),
    )

    private fun randomInstant() = Instant.fromEpochMilliseconds(
        epochMilliseconds =
        Random.nextLong(
            from = Instant.DISTANT_PAST.toEpochMilliseconds(),
            until = Instant.DISTANT_FUTURE.toEpochMilliseconds(),
        ),
    )
}
