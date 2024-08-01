package com.zegreatrob.tools.digger

import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.core.SemverType
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDigTest {
    @Test
    fun canGetAllGroupsFromRegex() {
        val input =
            "[Cowdog-42] -3- I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
        val (storyId, ease, coAuthors) = MessageDigger().digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = 3, actual = ease)
        assertEquals(
            expected = listOf("some@guy.io", "another@guy.io"),
            actual = coAuthors,
        )
    }

    @Test
    fun onlyCoAuthorsWorksAsIntended() {
        val input =
            "I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
        val (storyId, ease, coAuthors) = MessageDigger().digIntoMessage(input)

        assertEquals(
            expected = listOf("some@guy.io", "another@guy.io"),
            actual = coAuthors,
        )
        assertEquals(expected = null, actual = storyId)
        assertEquals(expected = null, actual = ease)
    }

    @Test
    fun onlyEaseWorksAsIntended() {
        val input = "-3- I did that thing"
        val (storyId, ease, coAuthors) = MessageDigger().digIntoMessage(input)

        assertEquals(expected = 3, actual = ease)
        assertEquals(expected = null, actual = storyId)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun multipleEaseReturnsFirst() {
        val input = "-3- -4- -5- I did that thing"
        val (storyId, ease, coAuthors) = MessageDigger().digIntoMessage(input)

        assertEquals(expected = 3, actual = ease)
        assertEquals(expected = null, actual = storyId)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun onlyStoryWorksAsIntended() {
        val input = "[Cowdog-42] I did that thing"
        val (storyId, ease, coAuthors) = MessageDigger().digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = null, actual = ease)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun onlyMajorWorksAsIntended() {
        val input = "[major] I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = SemverType.Major, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun onlyMinorWorksAsIntended() {
        val input = "[minor] I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = SemverType.Minor, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun whenIncludingMajorMultipleSemverTagsRespectsLargestOne() {
        val input = "[minor] [major] [none] [patch] I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = SemverType.Major, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun whenIncludingMinorMultipleSemverTagsRespectsLargestOne() {
        val input = "[minor] [none] [patch] I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = SemverType.Minor, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun whenIncludingPatchMultipleSemverTagsRespectsLargestOne() {
        val input = "[none] [patch] I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = SemverType.Patch, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun onlyPatchWorksAsIntended() {
        val input = "[patch] I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = SemverType.Patch, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun onlyNoneWorksAsIntended() {
        val input = "[none] I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = SemverType.None, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternateMajorRegexWillCorrectlyIdentityTags() {
        val messageDigger =
            MessageDigger(
                majorRegex = Regex("\\(.*big.*\\)"),
            )
        val input = "commit (big) 2"
        val result = messageDigger.digIntoMessage(input)

        assertEquals(expected = SemverType.Major, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternateMinorRegexWillCorrectlyIdentityTags() {
        val messageDigger =
            MessageDigger(
                minorRegex = Regex("\\(.*middle.*\\)"),
            )
        val input = "commit (middle) 2"
        val result = messageDigger.digIntoMessage(input)

        assertEquals(expected = SemverType.Minor, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternatePatchRegexWillCorrectlyIdentityTags() {
        val messageDigger =
            MessageDigger(
                patchRegex = Regex("\\(.*widdle.*\\)"),
            )
        val input = "commit (widdle) 2"
        val result = messageDigger.digIntoMessage(input)

        assertEquals(expected = SemverType.Patch, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternateNoneRegexWillCorrectlyIdentityTags() {
        val messageDigger =
            MessageDigger(
                noneRegex = Regex("\\(no\\)"),
            )
        val input = "commit (no) 2"
        val result = messageDigger.digIntoMessage(input)

        assertEquals(expected = SemverType.None, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun multipleStoryPrefersFirst() {
        val input = "[Cowdog-42] [otherStuff] [Eeeeee] I did that thing"
        val (storyId, ease, coAuthors) = MessageDigger(Regex("\\(.*big.*\\)")).digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = null, actual = ease)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun includingStoryIdAndPatchWorksAsExpected() {
        val input = "[Cowdog-42] [patch] I did that thing"
        val result = MessageDigger(Regex("\\(.*big.*\\)")).digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = result.storyId)
        assertEquals(expected = SemverType.Patch, actual = result.semver)
    }

    @Test
    fun includingMajorAndStoryIdWorksAsExpected() {
        val input = "[major] [Cowdog-42]  I did that thing"
        val result = MessageDigger().digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = result.storyId)
        assertEquals(expected = SemverType.Major, actual = result.semver)
    }
}
