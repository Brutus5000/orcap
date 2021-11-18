package net.brutus5000.orcap.shared

import net.brutus5000.orcap.OrcapProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.stream.Stream

private const val REQUIRED_SCOPE = "lobby"
private const val CONTENT_DIRECTORY = "./content"

class PathResolverTest {
    private lateinit var orcapProperties: OrcapProperties
    private lateinit var underTest: PathResolver

    @BeforeEach
    fun beforeEach() {
        orcapProperties = OrcapProperties(
            REQUIRED_SCOPE,
            CONTENT_DIRECTORY,
            null,
        )

        underTest = PathResolver(orcapProperties)
    }

    @DisplayName("Pass on resolving a valid subpath")
    @ParameterizedTest
    @MethodSource("resolveValidSubPathInput")
    fun resolveValidSubPath(subPath: String, expectedPath: Path) {
        val result = underTest.resolveFilePath(subPath)

        Assertions.assertEquals(expectedPath, result)
    }

    @DisplayName("Fail on resolving a subpath trying to navigate up")
    @Test
    fun resolveSubPathNavigatingUp() {
        Assertions.assertThrows(IllegalPathException::class.java) {
            underTest.resolveFilePath("../some/valid/subpath.txt")
        }
    }

    @DisplayName("Fail on resolving an empty subpath")
    @Test
    fun resolveEmptySubPath() {
        Assertions.assertThrows(IllegalPathException::class.java) {
            underTest.resolveFilePath("")
        }
    }

    companion object {
        @JvmStatic
        fun resolveValidSubPathInput(): Stream<Arguments> = listOf(
            Arguments.of("rootlevelfile.txt", Path.of("content/rootlevelfile.txt")),
            Arguments.of("down/../rootlevelfile.txt", Path.of("content/rootlevelfile.txt")),
            Arguments.of("some/valid/subpath.txt", Path.of("content/some/valid/subpath.txt")),
        ).stream()
    }
}
