
package com.my.kizzy.rpc

import com.my.kizzy.repository.KizzyRepository

sealed class RpcImage {
    fun toUrl(): String? = (this as? ExternalImage)?.image

    companion object {
        suspend fun resolveImages(
            repository: KizzyRepository,
            images: List<RpcImage?>
        ): List<String?> {
            val externalUrls = images.mapNotNull { it?.toUrl() }
            val resolvedUrls = repository.getImages(externalUrls) ?: emptyList()
            val resolvedUrlMap = externalUrls.zip(resolvedUrls).toMap()

            return images.map {
                when (it) {
                    is DiscordImage -> "mp:${it.image}"
                    is ExternalImage -> resolvedUrlMap[it.image]
                    null -> null
                }
            }
        }
    }

    class DiscordImage(val image: String) : RpcImage()
    class ExternalImage(val image: String) : RpcImage()
}
