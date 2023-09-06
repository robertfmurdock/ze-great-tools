package com.zegreatrob.tools.digger

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDigTest {

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

    @Test
    fun onlyCoAuthorsWorksAsIntended() {
        val input =
            "I did that thing\nCo-authored-by: Some Guy <some@guy.io>\nCo-authored-by: Another Guy <another@guy.io>"
        val (storyId, ease, coAuthors) = digIntoMessage(input)

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
        val (storyId, ease, coAuthors) = digIntoMessage(input)

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
        val (storyId, ease, coAuthors) = digIntoMessage(input)

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
        val (storyId, ease, coAuthors) = digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = null, actual = ease)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }

    @Test
    fun multipleStoryPrefersFirst() {
        val input = "[Cowdog-42] [otherStuff] [Eeeeee] I did that thing"
        val (storyId, ease, coAuthors) = digIntoMessage(input)

        assertEquals(expected = "Cowdog-42", actual = storyId)
        assertEquals(expected = null, actual = ease)
        assertEquals(
            expected = emptyList(),
            actual = coAuthors,
        )
    }
}
