package com.zegreatrob.tools.adapter.git

expect fun runProcess(args: List<String>, workingDirectory: String, env: Map<String, String> = mapOf()): String
