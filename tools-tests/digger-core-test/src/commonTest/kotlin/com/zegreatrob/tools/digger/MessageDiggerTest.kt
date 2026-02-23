package com.zegreatrob.tools.digger

import com.zegreatrob.minassert.assertIsEqualTo
import com.zegreatrob.testmints.setup
import com.zegreatrob.tools.digger.core.MessageDigger
import com.zegreatrob.tools.digger.core.SemverType
import kotlin.test.Test

class MessageDiggerTest {
    @Test
    fun canGetAllGroupsFromRegex() = setup(object {
        val input =
            "[Cowdog-42] -3- I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        storyId.assertIsEqualTo("Cowdog-42")
        ease.assertIsEqualTo(3)
        coAuthors.assertIsEqualTo(listOf("some@guy.io", "another@guy.io"))
    }

    @Test
    fun onlyCoAuthorsWorksAsIntended() = setup(object {
        val input =
            "I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        coAuthors.assertIsEqualTo(listOf("some@guy.io", "another@guy.io"))
        storyId.assertIsEqualTo(null)
        ease.assertIsEqualTo(null)
    }

    @Test
    fun onlyEaseWorksAsIntended() = setup(object {
        val input = "-3- I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        ease.assertIsEqualTo(3)
        storyId.assertIsEqualTo(null)
        coAuthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun multipleEaseReturnsFirst() = setup(object {
        val input = "-3- -4- -5- I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        ease.assertIsEqualTo(3)
        storyId.assertIsEqualTo(null)
        coAuthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun onlyStoryWorksAsIntended() = setup(object {
        val input = "[Cowdog-42] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        storyId.assertIsEqualTo("Cowdog-42")
        ease.assertIsEqualTo(null)
        coAuthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun onlyMajorWorksAsIntended() = setup(object {
        val input = "[major] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.semver.assertIsEqualTo(SemverType.Major)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun onlyMinorWorksAsIntended() = setup(object {
        val input = "[minor] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.semver.assertIsEqualTo(SemverType.Minor)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun whenIncludingMajorMultipleSemverTagsRespectsLargestOne() = setup(object {
        val input = "[minor] [major] [none] [patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.semver.assertIsEqualTo(SemverType.Major)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun whenIncludingMinorMultipleSemverTagsRespectsLargestOne() = setup(object {
        val input = "[minor] [none] [patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.semver.assertIsEqualTo(SemverType.Minor)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun whenIncludingPatchMultipleSemverTagsRespectsLargestOne() = setup(object {
        val input = "[none] [patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.semver.assertIsEqualTo(SemverType.Patch)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun onlyPatchWorksAsIntended() = setup(object {
        val input = "[patch] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.semver.assertIsEqualTo(SemverType.Patch)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun onlyNoneWorksAsIntended() = setup(object {
        val input = "[none] I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.semver.assertIsEqualTo(SemverType.None)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
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
        result.semver.assertIsEqualTo(SemverType.Major)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
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
        result.semver.assertIsEqualTo(SemverType.Minor)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
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
        result.semver.assertIsEqualTo(SemverType.Patch)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
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
        result.semver.assertIsEqualTo(SemverType.None)
        result.storyId.assertIsEqualTo(null)
        result.coauthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun multipleStoryPrefersFirst() = setup(object {
        val input = "[Cowdog-42] [otherStuff] [Eeeeee] I did that thing"
    }) exercise {
        MessageDigger(Regex("\\(.*big.*\\)")).digIntoMessage(input)
    } verify { (storyId, ease, coAuthors) ->
        storyId.assertIsEqualTo("Cowdog-42")
        ease.assertIsEqualTo(null)
        coAuthors.assertIsEqualTo(emptyList())
    }

    @Test
    fun includingStoryIdAndPatchWorksAsExpected() = setup(object {
        val input = "[Cowdog-42] [patch] I did that thing"
    }) exercise {
        MessageDigger(Regex("\\(.*big.*\\)")).digIntoMessage(input)
    } verify { result ->
        result.storyId.assertIsEqualTo("Cowdog-42")
        result.semver.assertIsEqualTo(SemverType.Patch)
    }

    @Test
    fun includingMajorAndStoryIdWorksAsExpected() = setup(object {
        val input = "[major] [Cowdog-42]  I did that thing"
    }) exercise {
        MessageDigger().digIntoMessage(input)
    } verify { result ->
        result.storyId.assertIsEqualTo("Cowdog-42")
        result.semver.assertIsEqualTo(SemverType.Major)
    }

    @Test
    fun settingStoryRegexWithoutGroupWillFail() = setup(object {
        @Suppress("RegExpRedundantEscape")
        val badRegex = Regex("\\[.*\\]")
    }) exercise {
        kotlin.runCatching { MessageDigger(storyIdRegex = badRegex) }
            .exceptionOrNull()
    } verify { result ->
        result?.message.assertIsEqualTo(
            "StoryIdRegex must include a storyId group. The regex was: ${badRegex.pattern}",
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
        result?.message.assertIsEqualTo(
            "EaseRegex must include an ease group. The regex was: ${badRegex.pattern}",
        )
    }
}
