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

import com.my.kizzy.remote.ApiResult
import com.my.kizzy.remote.ApiService
import com.my.kizzy.utils.toImageAssets

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImages(urls: List<String>): List<ApiResult>? {
        val result = api.getImages(urls)
        return result.map { apiResponse ->
            println("KizzyRepository Debug: Successfully parsed ApiResponse: $apiResponse")
            val assets = apiResponse.toImageAssets(urls)
            println("KizzyRepository Debug: Converted to ImageAssets: $assets")
            assets
        }.onFailure { exception ->
            println("KizzyRPC Error: Failed to get images. Exception: ${exception.message}")
            exception.printStackTrace()
        }.getOrNull()
    }
}
