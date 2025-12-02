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

import com.my.kizzy.remote.ApiResponse
import com.my.kizzy.remote.ApiResult
import com.my.kizzy.rpc.RpcImage

fun ApiResponse.toImageAssets(originalUrls: List<String>): List<ApiResult> {
    val count = minOf(id.size, originalUrls.size)
    return (0 until count).map { index ->
        ApiResult(
            id = id[index],
            originalUrl = originalUrls[index]
        )
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
