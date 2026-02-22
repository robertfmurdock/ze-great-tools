@file:OptIn(ExperimentalUuidApi::class)

package com.zegreatrob.tools.digger.json

import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.digger.model.Contribution
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ContributionParserTest {
    @Test
    fun canRoundTripSuccessfully() = setup(object {
        val contribution = stubContribution()
    }) exercise {
        ContributionParser.parseContribution(contribution.toJsonString())
    } verify { result ->
        assertEquals(contribution, result)
    }

    @Test
    fun willIgnoreExtraAttributes() = setup(object {
        val contribution = stubContribution()
        val jsonString = contribution.toJsonString()
        val jsonElement = Json.parseToJsonElement(jsonString)
        val augmentedJsonElement = buildJsonObject {
            jsonElement.jsonObject.forEach {
                put(it.key, it.value)
            }
            put("extraExtra", "readAllAboutIt")
        }
    }) exercise {
        ContributionParser.parseContribution(augmentedJsonElement.toString())
    } verify { result ->
        assertEquals(contribution, result)
    }

    @Test
    fun willTolerateMissingAttributes() = setup(object {
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
    }) exercise {
        ContributionParser.parseContribution(augmentedJsonElement.toString())
    } verify { result ->
        assertEquals(
            contribution.copy(
                dateTime = null,
                firstCommitDateTime = null,
                ease = null,
                storyId = null,
                semver = null,
                label = null,
            ),
            result,
        )
    }

    @Test
    fun canRoundTripListSuccessfully() = setup(object {
        val contributions = listOf(stubContribution(), stubContribution(), stubContribution())
    }) exercise {
        ContributionParser.parseContributions(contributions.toJsonString())
    } verify { result ->
        assertEquals(contributions, result)
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
