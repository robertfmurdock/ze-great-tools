package com.zegreatrob.tools.tagger.core

fun buildNoUpstreamError(): String = """
⚠️  HEAD has no upstream tracking branch (detached HEAD).

RISK: On release branches, this can produce stable versions that trigger unintended
production releases. Fix your CI checkout configuration.

See: https://github.com/robertfmurdock/ze-great-tools/blob/main/docs/tagger-detached-head.md
""".trimIndent()
