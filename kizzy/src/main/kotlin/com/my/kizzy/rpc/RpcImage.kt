
package com.my.kizzy.rpc

import com.my.kizzy.repository.KizzyRepository

sealed class RpcImage {
    fun toUrl(): String? {
        return when (this) {
            is DiscordImage -> null
            is ExternalImage -> image
        }
    }

    companion object {
        suspend fun resolveImages(
            repository: KizzyRepository,
            images: List<RpcImage?>
        ): List<String?> {
            val externalImages = images.mapNotNull { it?.toUrl() }
            val resolvedExternalImages = repository.getImages(externalImages)?.iterator()

            return images.map {
                when (it) {
                    is DiscordImage -> "mp:${it.image}"
                    is ExternalImage -> resolvedExternalImages?.next()
                    null -> null
                }
            }
        }
    }

    class DiscordImage(val image: String) : RpcImage()
    class ExternalImage(val image: String) : RpcImage()
}
