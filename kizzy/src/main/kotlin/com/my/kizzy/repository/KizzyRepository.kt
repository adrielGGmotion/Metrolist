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
import org.json.JSONObject

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        return api.getImage(url).getOrNull()?.toImageAsset()
    }

    suspend fun getBatchImages(urls: List<String>): Map<String, String>? {
        val jsonResponse = api.getBatchImages(urls).getOrNull() ?: return null
        val json = JSONObject(jsonResponse)
        val map = mutableMapOf<String, String>()
        for (key in json.keys()) {
            map[key] = json.getString(key)
        }
        return map
    }
}
