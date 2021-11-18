package net.brutus5000.orcap.shared

import reactor.core.publisher.Mono
import java.nio.file.Path

/**
 * Shared interface for a download service used by the DownloadController
 */
interface DownloadService {
    fun provideFileFromPath(subPath: String): Mono<Path>
    fun provideFileChecksum(subPath: String): Mono<Checksum>
}
