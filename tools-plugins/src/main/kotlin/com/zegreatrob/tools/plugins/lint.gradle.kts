package com.zegreatrob.tools.plugins

plugins {
    id("org.jmailen.kotlinter")
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(tasks.matching { it.name == "lintKotlin" })
}
