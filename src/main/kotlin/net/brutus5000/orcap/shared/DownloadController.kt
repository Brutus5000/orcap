package net.brutus5000.orcap.shared

import net.brutus5000.orcap.OrcapProperties
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

const val CHECKSUM_HEADER = "X-CRC32"
const val PARAM_CHECKSUM_ONLY = "checksumOnly"

/**
 * Controller to handle downloads of files
 */
@RestController
class DownloadController(
    val downloadService: DownloadService,
    orcapProperties: OrcapProperties,
) {
    val requiredScope = "SCOPE_" + orcapProperties.requiredScope

    /**
     * Download endpoint that passes through any file name including subdirectories.
     * Requires the configured scope from the properties.
     */
    @GetMapping("/{*fileName}")
    @PreAuthorize("hasAuthority(@downloadController.getRequiredScope())")
    fun downloadPatch(
        @PathVariable fileName: String,
        @RequestParam params: Map<String, Any>,
    ): Mono<ResponseEntity<Any>> {
        // Spring captures the leading slash (see Javadoc for class PathPattern)
        // We prepend a `.` to avoid pointing to absolute file paths
        val normalizedFileName = ".$fileName"

        return downloadService.provideFileFromPath(normalizedFileName)
            .flatMap { path ->
                downloadService.provideFileChecksum(normalizedFileName)
                    .map { checksum -> path to checksum }
            }
            .map { (path, checksum) ->
                ResponseEntity.ok()
                    .headers(HttpHeaders().apply { set(CHECKSUM_HEADER, checksum.toString()) })
                    .body(
                        if (params.containsKey(PARAM_CHECKSUM_ONLY)) ""
                        else FileSystemResource(path)
                    )
            }
            .onErrorResume {
                when (it) {
                    is UplinkNotAvailableException -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build<Any>()
                        .toMono()
                    is IllegalPathException -> (
                        ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(it.message!!) as ResponseEntity<Any>
                        ).toMono()
                    else -> Mono.error(it)
                }
            }
            .switchIfEmpty {
                ResponseEntity.notFound().build<Any>().toMono()
            }
    }
}
