package com.zegreatrob.tools.test.git

actual suspend fun delayLongEnoughToAffectGitDate() {
    sleep(1000)
}
