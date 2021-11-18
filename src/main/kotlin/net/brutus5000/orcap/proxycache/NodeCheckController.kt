package net.brutus5000.orcap.proxycache

import net.brutus5000.orcap.OrcapProperties
import net.brutus5000.orcap.PROXY_CACHE_PROFILE
import net.brutus5000.orcap.shared.NodeInfo
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile(PROXY_CACHE_PROFILE)
class NodeCheckController(
    orcapProperties: OrcapProperties
) {
    private val uplinkConfig = orcapProperties.proxyCacheConfig!!

    @GetMapping("/nodeCheck")
    fun nodeCheck() = NodeInfo(
        uplinkConfig.nodeId,
        uplinkConfig.selfUrl,
        uplinkConfig.uplinkUrl,
    )
}
