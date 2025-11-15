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
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Modified by Zion Huang
 */
class ApiService {
    private val client: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            install(HttpCache)
        }
    }

    suspend fun getImage(url: String) = runCatching {
        println("--- ApiService.getImage ENTER ---")
        val response = try {
            client.get {
                url("$BASE_URL/image")
                parameter("url", url)
            }
        } catch (e: Exception) {
            println("!!! ApiService.getImage ERROR calling client.get: ${e.message}")
            e.printStackTrace()
            throw e
        }
        val responseBody = response.bodyAsText()
        println("Raw response from worker: $responseBody")
        println("--- ApiService.getImage EXIT ---")
        response
    }

    companion object {
        const val BASE_URL = "https://metrolist-discord-rpc-api.adrieldsilvas-2.workers.dev"
    }
}
