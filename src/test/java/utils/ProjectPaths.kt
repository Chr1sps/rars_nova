package utils

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.toPath

object ProjectPaths {

    val projectRoot: Path
        get() {
            var directory: Path? = projectRootByClass
            while (directory != null) {
                try {
                    val children = Files.list(directory).use {
                        it.map(Path::name).collect(Collectors.toSet())
                    }
                    if (children.contains("gradle") && children.contains("src")) {
                        return directory
                    }
                } catch (_: IOException) {
                }
                directory = directory.parent
            }
            error("Could not find project root")
        }

    private val projectRootByClass: Path
        get() {
            val replacedName = javaClass.name.replace(".", "/")
            val url = javaClass.getResource("/$replacedName.class")
            return when (url?.protocol) {
                "file" -> url.toURI().toPath()
                "jar" -> url.toURI().toPath().pathString.split("!").firstOrNull()?.let { Path.of(it) }
                else -> error("Unsupported protocol")
            } ?: error("Could not find project root")
        }
}
