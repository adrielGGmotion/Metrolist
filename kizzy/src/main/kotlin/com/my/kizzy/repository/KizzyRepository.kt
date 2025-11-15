/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRepositoryImpl.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.repository

import com.my.kizzy.remote.ApiService
import com.my.kizzy.utils.toImageAsset
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        logger.debug { "--- KizzyRepository.getImage ENTER ---" }
        logger.debug { "getImage called with url: $url" }
        val response = api.getImage(url).getOrNull()
        logger.debug { "api.getImage response: $response" }
        val asset = response?.toImageAsset()
        logger.debug { "toImageAsset result: $asset" }
        logger.debug { "--- KizzyRepository.getImage EXIT ---, result: $asset" }
        return asset
    }
}
