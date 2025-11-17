/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * RpcImage.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.rpc

import com.my.kizzy.repository.KizzyRepository

/**
 * Modified by Zion Huang
 */
sealed class RpcImage {
    abstract fun getUrl(): String

    class DiscordImage(val image: String) : RpcImage() {
        override fun getUrl(): String {
            return "mp:${image}"
        }
    }

    class ExternalImage(val image: String) : RpcImage() {
        override fun getUrl(): String {
            return image
        }
    }

    companion object {
        suspend fun resolveImages(
            repository: KizzyRepository,
            images: List<RpcImage?>,
        ): List<String?> {
            val externalImages = images.map {
                if (it is ExternalImage) it.getUrl() else null
            }
            if (externalImages.any { it != null }) {
                val resolvedImages = repository.getImages(
                    externalImages.filterNotNull()
                ).getOrNull()?.id
                var resolvedImageIndex = 0
                return images.map {
                    when (it) {
                        is DiscordImage -> it.getUrl()
                        is ExternalImage -> resolvedImages?.getOrNull(resolvedImageIndex++)
                        null -> null
                    }
                }
            }
            return images.map {
                if (it is DiscordImage) it.getUrl() else null
            }
        }
    }
}
