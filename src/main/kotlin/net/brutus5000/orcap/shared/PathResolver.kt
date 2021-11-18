package net.brutus5000.orcap.shared

import net.brutus5000.orcap.OrcapProperties
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.jvm.Throws

@Component
class PathResolver(
    val orcapProperties: OrcapProperties
) {
    @Throws(IllegalPathException::class)
    fun resolveFilePath(subPath: String): Path {
        val root = Path.of(orcapProperties.contentDirectory).normalize()
        val effectivePath = root.resolve(subPath).normalize()

        return when {
            root == effectivePath -> throw IllegalPathException("The subPath is effectively empty", subPath)
            !effectivePath.contains(root) -> throw IllegalPathException(
                "The path tries to navigate outside of the content directory",
                subPath
            )
            effectivePath.isDirectory() -> throw IllegalPathException("Pointing to a directory", subPath)
            else -> effectivePath
        }
    }
}
