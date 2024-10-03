package com.zegreatrob.tools.test.git

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

actual fun removeDirectory(directoryPath: String) {
    Files.walk(Paths.get(directoryPath)).use { pathStream ->
        pathStream.sorted(Comparator.reverseOrder())
            .map { obj: Path -> obj.toFile() }
            .forEach { obj: File -> obj.delete() }
    }
}
