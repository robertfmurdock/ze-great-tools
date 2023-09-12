package com.zegreatrob.tools.digger.json

import com.benasher44.uuid.uuid4
import com.zegreatrob.tools.digger.model.Contribution
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
        lastCommit = "${uuid4()}",
        firstCommit = "${uuid4()}",
        authors = listOf("${uuid4()}"),
        dateTime = "${uuid4()}",
        ease = Random.nextInt(),
        storyId = "${uuid4()}",
        semver = "${uuid4()}",
    )
}
