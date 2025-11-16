/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * ApiService.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */
package com.my.kizzy.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Modified by Zion Huang
 */
class ApiService {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpCache)
    }

    suspend fun getImages(urls: List<String>): Result<ApiResponse> {
        var result: Result<ApiResponse>? = null
        for (baseUrl in WORKERS) {
            result = runCatching {
                client.get {
                    url("$baseUrl/image")
                    urls.forEach {
                        parameter("url", it)
                    }
                }.body()
            }
            if (result.isSuccess) {
                return result
            }
        }
        return result ?: Result.failure(Exception("All workers failed"))
    }

    companion object {
        private val WORKERS = listOf(
            "https://metrolist-discord-rpc-api.adrieldsilvas-2.workers.dev",
            "https://kizzy-workers.astolfo.in"
        )
    }
}
