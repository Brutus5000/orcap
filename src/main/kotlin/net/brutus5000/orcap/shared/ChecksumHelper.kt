package net.brutus5000.orcap.shared

import reactor.core.publisher.Mono
import java.nio.file.Path
import java.util.zip.CRC32
import kotlin.io.path.inputStream

typealias Checksum = Long

object ChecksumHelper {
    private const val BUFFER_SIZE = 4096

    fun calcChecksumAsync(path: Path): Mono<Checksum> = Mono.fromCallable {
        calcChecksum(path)
    }

    private fun calcChecksum(path: Path): Checksum {
        val crc32 = CRC32()
        path.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (input.read(buffer).apply { bytesRead = this } > 0) {
                crc32.update(buffer, 0, bytesRead)
            }
        }

        return crc32.value
    }
}
