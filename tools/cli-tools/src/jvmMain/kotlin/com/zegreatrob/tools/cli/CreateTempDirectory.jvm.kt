package com.zegreatrob.tools.cli

import java.nio.file.Files
import kotlin.io.path.absolutePathString

actual fun createTempDirectory(): String = Files.createTempDirectory("zgt").absolutePathString()
