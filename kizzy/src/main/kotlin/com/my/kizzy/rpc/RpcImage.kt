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
    abstract suspend fun resolveImage(repository: KizzyRepository): String?

    class DiscordImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String {
            return "mp:${image}"
        }
    }

    class ExternalImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            return repository.getImages(listOf(image))?.firstOrNull()
        }
    }

    companion object {
        suspend fun resolveImages(
            repository: KizzyRepository,
            images: List<RpcImage?>
        ): List<String?> {
            val externalImages = images.filterIsInstance<ExternalImage>()
            val resolvedExternalImages = if (externalImages.isNotEmpty()) {
                repository.getImages(externalImages.map { it.image })
            } else {
                emptyList()
            }

            val resolvedImages = mutableListOf<String?>()
            var externalImageIndex = 0
            for (image in images) {
                when (image) {
                    is DiscordImage -> resolvedImages.add(image.resolveImage(repository))
                    is ExternalImage -> {
                        resolvedImages.add(resolvedExternalImages?.getOrNull(externalImageIndex))
                        externalImageIndex++
                    }
                    null -> resolvedImages.add(null)
                }
            }
            return resolvedImages
        }
    }
}
