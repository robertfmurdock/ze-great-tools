package com.zegreatrob.tools.digger.json

import com.zegreatrob.tools.digger.core.Contribution
import java.util.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ContributionParserTest {

    @Test
    fun canRoundTripSuccessfully() {
        val contribution = stubContribution()
        val loaded = ContributionParser.parseContribution(contribution.toJsonString())
        assertEquals(contribution, loaded)
    }

    @Test
    fun canRoundTripListSuccessfully() {
        val contributions = listOf(stubContribution(), stubContribution(), stubContribution())
        val loaded = ContributionParser.parseContributions(contributions.toJsonString())
        assertEquals(contributions, loaded)
    }

    private fun stubContribution() = Contribution(
        lastCommit = UUID.randomUUID().toString(),
        firstCommit = UUID.randomUUID().toString(),
        authors = listOf(UUID.randomUUID().toString()),
        dateTime = UUID.randomUUID().toString(),
        ease = Random.nextInt(),
        storyId = UUID.randomUUID().toString(),
        semver = UUID.randomUUID().toString(),
    )
}
