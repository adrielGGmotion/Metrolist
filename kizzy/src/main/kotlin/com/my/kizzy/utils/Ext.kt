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

import com.my.kizzy.remote.ImagePayload
import com.my.kizzy.rpc.RpcImage
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun HttpResponse.toImageAsset(): String? {
    return try {
        if (this.status == HttpStatusCode.OK) {
            val bodyAsText = this.bodyAsText()
            logger.debug { "Worker response: $bodyAsText" }
            this.body<ImagePayload>().id.firstOrNull()
        }
        else
            null
    } catch (e: Exception) {
        logger.error(e) { "Failed to parse worker response" }
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

