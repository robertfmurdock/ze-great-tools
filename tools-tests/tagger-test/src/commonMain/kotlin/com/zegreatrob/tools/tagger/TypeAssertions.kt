package com.zegreatrob.tools.tagger

import kotlin.test.assertIs

inline fun <reified TExpected> Any?.assertIsOfType(): TExpected = assertIs<TExpected>(this)
