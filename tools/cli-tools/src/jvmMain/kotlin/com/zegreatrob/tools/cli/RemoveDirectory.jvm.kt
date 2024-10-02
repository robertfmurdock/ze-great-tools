package com.zegreatrob.tools.cli

import java.nio.file.Files
import java.nio.file.Paths

actual fun removeDirectory(directoryPath: String) {
    Files.deleteIfExists(Paths.get(directoryPath))
}
