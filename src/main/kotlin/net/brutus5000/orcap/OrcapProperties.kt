package net.brutus5000.orcap

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

data class ProxyCacheProperties(
    val uplinkUrl: String,
    val selfUrl: String,
    val nodeId: String,
    val region: String,
)

@ConfigurationProperties("orcap")
@ConstructorBinding
data class OrcapProperties(
    /**
     * The scope required to get access to the content
     */
    val requiredScope: String,

    /**
     * Path to directory where files are stored (excluding patch files)
     */
    val contentDirectory: String,

    /**
     * Uplink setup if profile is active
     */
    val proxyCacheConfig: ProxyCacheProperties?
)
