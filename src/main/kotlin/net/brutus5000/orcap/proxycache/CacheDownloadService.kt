package net.brutus5000.orcap.proxycache

import net.brutus5000.orcap.OrcapProperties
import net.brutus5000.orcap.PROXY_CACHE_PROFILE
import net.brutus5000.orcap.shared.CHECKSUM_HEADER
import net.brutus5000.orcap.shared.Checksum
import net.brutus5000.orcap.shared.DownloadService
import net.brutus5000.orcap.shared.PARAM_CHECKSUM_ONLY
import net.brutus5000.orcap.shared.PathResolver
import net.brutus5000.orcap.shared.UplinkNotAvailableException
import org.springframework.context.annotation.Profile
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ServerWebExchange
import reactor.cache.CacheMono
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap

private fun WebClient.Builder.forwardAuthorizationHeader(): WebClient.Builder =
    this.filter { request: ClientRequest, next: ExchangeFunction ->
        Mono.deferContextual {
            val incomingRequest = it.get(ServerWebExchange::class.java).request
            val authorization = incomingRequest.headers.getFirst(HttpHeaders.AUTHORIZATION)
            val newRequest = ClientRequest.from(request)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .build()
            next.exchange(newRequest)
        }
    }

@Service
@Profile(PROXY_CACHE_PROFILE)
class CacheDownloadService(
    private val orcapProperties: OrcapProperties,
    private val pathResolver: PathResolver,
    webClientBuilder: WebClient.Builder,
) : DownloadService {
    private val uplinkConfig = orcapProperties.proxyCacheConfig!!
    private val webClient = webClientBuilder
        .forwardAuthorizationHeader()
        .build()

    private val fileChecksumCache: MutableMap<String, Checksum> = ConcurrentHashMap()

    override fun provideFileFromPath(subPath: String): Mono<Path> = CacheMono
        .lookup({ key ->
            Mono.justOrEmpty<Checksum>(fileChecksumCache[key])
                .map { Signal.next(pathResolver.resolveFilePath(subPath) to it) }
        }, subPath)
        .onCacheMissResume {
            getFileFromUplink(subPath)
        }
        .andWriteWith { _, signal ->
            if (signal.isOnError) return@andWriteWith Mono.error(signal.throwable!!)

            Mono.fromRunnable {
                val (_, checksum) = signal.get() ?: throw IllegalStateException("Checksum is null")
                fileChecksumCache[subPath] = checksum
            }
        }
        .map { (path, _) -> path }

    override fun provideFileChecksum(subPath: String): Mono<Checksum> = CacheMono
        .lookup({ key ->
            Mono.justOrEmpty<Checksum>(fileChecksumCache[key])
                .map { Signal.next(it) }
        }, subPath)
        .onCacheMissResume {
            getChecksumFromUplink(subPath)
        }
        .andWriteWith { k, signal ->
            if (signal.isOnError) return@andWriteWith Mono.error(signal.throwable!!)

            Mono.fromRunnable {
                val checksum = signal.get() ?: throw IllegalStateException("Checksum is null")
                fileChecksumCache[k] = checksum
            }
        }

    private fun getFileFromUplink(subPath: String): Mono<Pair<Path, Checksum>> =
        webClient.get()
            .uri("${uplinkConfig.uplinkUrl}/$subPath?")
            .exchangeToMono { response ->
                val checksum = checkNotNull(
                    response.headers().header(CHECKSUM_HEADER).firstOrNull()?.toLong()
                ) { "Uplink server did not respond with checksum for subPath: $subPath" }

                Mono.just(
                    checksum to response.bodyToFlux(DataBuffer::class.java)
                )
            }
            .onErrorResume {
                when (it) {
                    is WebClientRequestException -> Mono.error(UplinkNotAvailableException())
                    is WebClientResponseException.NotFound -> Mono.empty()
                    else -> Mono.error(it)
                }
            }
            .flatMap { (checksum, dataBuffer) ->
                val path = pathResolver.resolveFilePath(subPath)

                DataBufferUtils.write(
                    dataBuffer,
                    path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )
                    .then(Mono.just(path to checksum))
            }

    private fun getChecksumFromUplink(subPath: String): Mono<Checksum> =
        webClient.get()
            .uri("${uplinkConfig.uplinkUrl}/$subPath?$PARAM_CHECKSUM_ONLY")
            .retrieve()
            .toEntity(String::class.java)
            .map {
                checkNotNull(
                    it.headers[CHECKSUM_HEADER]?.firstOrNull()?.toLong()
                ) { "Uplink server did not respond with checksum for subPath: $subPath" }
            }
            .onErrorResume {
                when (it) {
                    is WebClientRequestException -> Mono.error(UplinkNotAvailableException())
                    is WebClientResponseException.NotFound -> Mono.empty()
                    else -> Mono.error(it)
                }
            }
}
