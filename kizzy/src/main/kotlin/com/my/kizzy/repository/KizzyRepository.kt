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

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        println("--- KizzyRepository.getImage ENTER ---")
        println("KizzyRepository.getImage called with url: $url")
        val response = try {
            api.getImage(url).getOrNull()
        } catch (e: Exception) {
            println("!!! KizzyRepository.getImage ERROR calling api.getImage: ${e.message}")
            e.printStackTrace()
            null
        }
        println("KizzyRepository.getImage api.getImage response: $response")
        val asset = try {
            response?.toImageAsset()
        } catch (e: Exception) {
            println("!!! KizzyRepository.getImage ERROR calling toImageAsset: ${e.message}")
            e.printStackTrace()
            null
        }
        println("KizzyRepository.getImage toImageAsset result: $asset")
        println("--- KizzyRepository.getImage EXIT ---, result: $asset")
        return asset
    }
}
