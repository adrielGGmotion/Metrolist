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

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImages(urls: List<String>): List<String?>? {
        return api.getImage(urls).getOrNull()?.id
    }
}
