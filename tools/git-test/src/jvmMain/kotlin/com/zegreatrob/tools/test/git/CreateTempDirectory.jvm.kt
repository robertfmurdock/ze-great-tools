package com.zegreatrob.tools.test.git

import java.nio.file.Files
import kotlin.io.path.absolutePathString

actual fun createTempDirectory(): String = Files.createTempDirectory("zgt").absolutePathString()
