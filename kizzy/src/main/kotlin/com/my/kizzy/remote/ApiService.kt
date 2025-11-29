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
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

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

    suspend fun getImage(url: String) = runCatching {
        client.get {
            url("${getNextWorker()}/image")
            parameter("url", url)
        }
    }

    companion object {
        private val WORKERS = listOf(
            "https://metrolist-discord-rpc-api.fullerbread2032.workers.dev",
            "https://metrolist-discord-rpc-api.adrieldsilvas-2.workers.dev"
        )
        private val currentIndex = AtomicInteger(0)

        fun getNextWorker(): String {
            val index = currentIndex.getAndIncrement()
            return WORKERS[index % WORKERS.size]
        }
    }
}
