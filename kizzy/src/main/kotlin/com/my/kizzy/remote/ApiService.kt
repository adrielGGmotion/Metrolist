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
             url("${BASE_URLS.random()}/image")
             parameter("url", url)
         }
    }

    suspend fun getBatchImages(urls: List<String>) = runCatching {
        client.get {
            url("${BASE_URLS.random()}/batch")
            urls.forEach {
                parameter("url", it)
            }
        }.bodyAsText()
    }

    companion object {
        private val BASE_URLS = listOf(
            "https://fullerbread2032-worker.workers.dev",
            "https://metrolist.adrieldsilvas-2.workers.dev"
        )
    }
}
