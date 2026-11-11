package com.zegreatrob.tools.plugins

plugins {
    id("com.zegreatrob.tools.plugins.lint")
    id("com.zegreatrob.tools.plugins.reports")
    id("com.zegreatrob.tools.plugins.testmints")
}

extra["testmints.includeCommonMain"] = true
