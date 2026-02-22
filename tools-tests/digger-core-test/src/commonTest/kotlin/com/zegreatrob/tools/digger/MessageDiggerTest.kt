package com.zegreatrob.tools.digger

import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.core.SemverType
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDiggerTest {
    @Test
    fun canGetAllGroupsFromRegex() = setup(object {
        val input =
            "[Cowdog-42] -3- I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = 3, actual = ease)
        assertEquals(
            expected = listOf("some@guy.io", "another@guy.io"),
            actual = coAuthors,
        )
    }

    @Test
    fun onlyCoAuthorsWorksAsIntended() = setup(object {
        val input =
            "I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        assertEquals(
            expected = listOf("some@guy.io", "another@guy.io"),
            actual = coAuthors,
        )
        assertEquals(expected = null, actual = storyId)
        assertEquals(expected = null, actual = ease)
    }

    @Test
    fun onlyEaseWorksAsIntended() = setup(object {
        val input = "-3- I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        assertEquals(expected = 3, actual = ease)
        assertEquals(expected = null, actual = storyId)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun multipleEaseReturnsFirst() = setup(object {
        val input = "-3- -4- -5- I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        assertEquals(expected = 3, actual = ease)
        assertEquals(expected = null, actual = storyId)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun onlyStoryWorksAsIntended() = setup(object {
        val input = "[Cowdog-42] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = null, actual = ease)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun onlyMajorWorksAsIntended() = setup(object {
        val input = "[major] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Major, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun onlyMinorWorksAsIntended() = setup(object {
        val input = "[minor] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Minor, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun whenIncludingMajorMultipleSemverTagsRespectsLargestOne() = setup(object {
        val input = "[minor] [major] [none] [patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Major, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun whenIncludingMinorMultipleSemverTagsRespectsLargestOne() = setup(object {
        val input = "[minor] [none] [patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Minor, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun whenIncludingPatchMultipleSemverTagsRespectsLargestOne() = setup(object {
        val input = "[none] [patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Patch, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun onlyPatchWorksAsIntended() = setup(object {
        val input = "[patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Patch, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun onlyNoneWorksAsIntended() = setup(object {
        val input = "[none] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.None, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternateMajorRegexWillCorrectlyIdentityTags() = setup(object {
        val messageDigger =
            MessageDigger(
                majorRegex = Regex("\\(.*big.*\\)"),
            )
        val input = "commit (big) 2"
    }) exercise {
        messageDigger.digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Major, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternateMinorRegexWillCorrectlyIdentityTags() = setup(object {
        val messageDigger =
            MessageDigger(
                minorRegex = Regex("\\(.*middle.*\\)"),
            )
        val input = "commit (middle) 2"
    }) exercise {
        messageDigger.digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Minor, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternatePatchRegexWillCorrectlyIdentityTags() = setup(object {
        val messageDigger =
            MessageDigger(
                patchRegex = Regex("\\(.*widdle.*\\)"),
            )
        val input = "commit (widdle) 2"
    }) exercise {
        messageDigger.digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.Patch, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun givenAlternateNoneRegexWillCorrectlyIdentityTags() = setup(object {
        val messageDigger =
            MessageDigger(
                noneRegex = Regex("\\(no\\)"),
            )
        val input = "commit (no) 2"
    }) exercise {
        messageDigger.digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = SemverType.None, actual = result.semver)
        assertEquals(expected = null, actual = result.storyId)
        assertEquals(
            expected = emptyList(),
            actual = result.coauthors,
        )
    }

    @Test
    fun multipleStoryPrefersFirst() = setup(object {
        val input = "[Cowdog-42] [otherStuff] [Eeeeee] I did that thing"
    }) exercise {
        MessageDigger(Regex("\\(.*big.*\\)")).digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = null, actual = ease)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun includingStoryIdAndPatchWorksAsExpected() = setup(object {
        val input = "[Cowdog-42] [patch] I did that thing"
    }) exercise {
        MessageDigger(Regex("\\(.*big.*\\)")).digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = "Cowdog-42", actual = result.storyId)
        assertEquals(expected = SemverType.Patch, actual = result.semver)
    }

    @Test
    fun includingMajorAndStoryIdWorksAsExpected() = setup(object {
        val input = "[major] [Cowdog-42]  I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        assertEquals(expected = "Cowdog-42", actual = result.storyId)
        assertEquals(expected = SemverType.Major, actual = result.semver)
    }

    @Test
    fun settingStoryRegexWithoutGroupWillFail() = setup(object {
        @Suppress("RegExpRedundantEscape")
        val badRegex = Regex("\\[.*\\]")
    }) exercise {
        kotlin.runCatching { MessageDigger(storyIdRegex = badRegex) }
            .exceptionOrNull()
    } verify { result ->
        assertEquals(
            expected = "StoryIdRegex must include a storyId group. The regex was: ${badRegex.pattern}",
            actual = result?.message,
        )
    }

    @Test
    fun settingEaseRegexWithoutGroupWillFail() = setup(object {
        @Suppress("RegExpRedundantEscape")
        val badRegex = Regex("\\[.*\\]")
    }) exercise {
        kotlin.runCatching { MessageDigger(easeRegex = badRegex) }
            .exceptionOrNull()
    } verify { result ->
        assertEquals(
            expected = "EaseRegex must include an ease group. The regex was: ${badRegex.pattern}",
            actual = result?.message,
        )
    }
}
