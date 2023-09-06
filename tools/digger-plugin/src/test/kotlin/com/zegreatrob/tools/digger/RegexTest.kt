package com.zegreatrob.tools.digger

import kotlin.test.Test
import kotlin.test.assertEquals

class RegexTest {

    @Test
    fun canGetAllGroupsFromRegex() {
        val input =
            "[Cowdog-42] -3- I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
        val (storyId, ease, coAuthors) = digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = 3, actual = ease)
        assertEquals(
            expected = listOf("some@guy.io", "another@guy.io"),
            actual = coAuthors,
        )
    }
}
