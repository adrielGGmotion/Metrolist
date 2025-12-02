/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * Ext.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.utils

import com.my.kizzy.remote.ApiResult
import com.my.kizzy.remote.BatchApiResponse
import com.my.kizzy.rpc.RpcImage
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

suspend fun HttpResponse.toImageAssets(): List<ApiResult>? {
    return try {
        if (this.status == HttpStatusCode.OK) {
            this.body<BatchApiResponse>().results
        } else {
            println("KizzyRPC Error: Received non-OK status code: ${this.status}")
            null
        }
    } catch (e: Exception) {
        println("KizzyRPC Error: Failed to parse response. Exception: ${e.message}")
        e.printStackTrace()
        null
    }
}

fun String.toRpcImage(): RpcImage? {
    return if (this.isBlank())
        null
    else if (this.startsWith("attachments"))
        RpcImage.DiscordImage(this)
    else
        RpcImage.ExternalImage(this)
}
