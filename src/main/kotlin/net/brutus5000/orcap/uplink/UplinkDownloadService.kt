package net.brutus5000.orcap.uplink

import net.brutus5000.orcap.UPLINK_PROFILE
import net.brutus5000.orcap.shared.Checksum
import net.brutus5000.orcap.shared.ChecksumHelper
import net.brutus5000.orcap.shared.DownloadService
import net.brutus5000.orcap.shared.PathResolver
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.cache.CacheMono
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import reactor.kotlin.core.publisher.toMono
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

@Service
@Profile(UPLINK_PROFILE)
class UplinkDownloadService(
    private val pathResolver: PathResolver
) : DownloadService {
    private val fileChecksumCache: MutableMap<String, Checksum> = ConcurrentHashMap()

    override fun provideFileChecksum(subPath: String): Mono<Checksum> = CacheMono
        .lookup({ key ->
            Mono.justOrEmpty<Checksum>(fileChecksumCache[key])
                .map { Signal.next(it) }
        }, subPath)
        .onCacheMissResume {
            ChecksumHelper.calcChecksumAsync(pathResolver.resolveFilePath(subPath))
        }
        .andWriteWith { k, signal ->
            Mono.fromRunnable {
                val checksum = signal.get() ?: throw IllegalStateException("Checksum is null")
                fileChecksumCache[k] = checksum
            }
        }

    override fun provideFileFromPath(subPath: String): Mono<Path> {
        val diskPath = try {
            pathResolver.resolveFilePath(subPath)
        } catch (ex: Exception) {
            return Mono.error(ex)
        }

        return when {
            Files.exists(diskPath) -> diskPath.toMono()
            else -> Mono.empty()
        }
    }
}
