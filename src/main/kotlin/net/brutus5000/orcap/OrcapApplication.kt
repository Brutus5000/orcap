package net.brutus5000.orcap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

const val PROXY_CACHE_PROFILE = "cache"
const val UPLINK_PROFILE = "uplink"

@SpringBootApplication
@EnableConfigurationProperties(OrcapProperties::class)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class OrcapApplication

fun main(args: Array<String>) {
    runApplication<OrcapApplication>(*args)
}
