package com.zegreatrob.tools.test.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

actual suspend fun delayLongEnoughToAffectGitDate() {
    withContext(Dispatchers.Default) {
        delay(1.seconds)
    }
}
