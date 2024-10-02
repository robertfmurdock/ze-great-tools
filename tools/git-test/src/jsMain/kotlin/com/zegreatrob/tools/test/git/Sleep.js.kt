package com.zegreatrob.tools.test.git

import kotlin.js.Date

actual fun sleep(millis: Long) {
    val start = Date.now()
    while (Date.now() - start < millis) {
        print("")
    }
}
