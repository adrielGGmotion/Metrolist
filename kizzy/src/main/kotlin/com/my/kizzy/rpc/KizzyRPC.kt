/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRPC.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.rpc

import com.my.kizzy.gateway.DiscordWebSocket
import com.my.kizzy.gateway.entities.presence.Activity
import com.my.kizzy.gateway.entities.presence.Assets
import com.my.kizzy.gateway.entities.presence.Metadata
import com.my.kizzy.gateway.entities.presence.Presence
import com.my.kizzy.gateway.entities.presence.Timestamps
import com.my.kizzy.repository.KizzyRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

/**
 * Modified by Zion Huang
 */
open class KizzyRPC(token: String) {
    private val kizzyRepository = KizzyRepository()
    private val discordWebSocket = DiscordWebSocket(token)
    private var lastSongId: String? = null
    private var cachedAssets: Assets? = null
    private var lastPresence: Presence? = null
    fun closeRPC() {
        discordWebSocket.close()
    }

    fun isRpcRunning(): Boolean {
        return discordWebSocket.isWebSocketConnected()
    }

    suspend fun stopActivity() {
        if (!isRpcRunning()) {
            discordWebSocket.connect()
        }
        val presence = Presence(
            activities = emptyList()
        )
        lastPresence = presence
        discordWebSocket.sendActivity(presence)
    }

    suspend fun forceUpdate() {
        lastPresence?.let {
            if (!isRpcRunning()) {
                discordWebSocket.connect()
            }
            discordWebSocket.sendActivity(it)
        }
    }

    suspend fun setActivity(
        name: String,
        state: String?,
        stateUrl: String? = null,
        details: String?,
        detailsUrl: String? = null,
        largeImage: RpcImage?,
        smallImage: RpcImage?,
        largeImage2: RpcImage?,
        smallImage2: RpcImage?,
        largeText: String? = null,
        smallText: String? = null,
        buttons: List<Pair<String, String>>? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        type: Type = Type.LISTENING,
        statusDisplayType: StatusDisplayType = StatusDisplayType.NAME,
        streamUrl: String? = null,
        applicationId: String? = null,
        status: String? = "online",
        since: Long? = null,
        songId: String? = null,
        force: Boolean = false
    ) {
        if (!isRpcRunning()) {
            discordWebSocket.connect()
        }

        val assets = if (songId != null && songId == lastSongId && cachedAssets != null && !force) {
            cachedAssets
        } else {
            val resolvedImages = RpcImage.resolveImages(
                repository = kizzyRepository,
                images = listOf(largeImage, smallImage, largeImage2, smallImage2)
            )
            val newAssets = if (resolvedImages.any { it != null }) {
                Assets(
                    largeImage = resolvedImages.getOrNull(0),
                    smallImage = resolvedImages.getOrNull(1),
                    largeText = largeText,
                    smallText = smallText
                )
            } else null
            lastSongId = songId
            cachedAssets = newAssets
            newAssets
        }

        val presence = Presence(
            activities = listOf(
                Activity(
                    name = name,
                    state = state,
                    stateUrl = stateUrl,
                    details = details,
                    detailsUrl = detailsUrl,
                    type = type.value,
                    statusDisplayType = statusDisplayType.value,
                    timestamps = Timestamps(startTime, endTime),
                    assets = assets,
                    buttons = buttons?.map { it.first },
                    metadata = Metadata(buttonUrls = buttons?.map { it.second }),
                    applicationId = applicationId.takeIf { !buttons.isNullOrEmpty() },
                    url = streamUrl
                )
            ),
            afk = true,
            since = since,
            status = status ?: "online"
        )
        lastPresence = presence
        discordWebSocket.sendActivity(presence)
    }

    enum class Type(val value: Int) {
        PLAYING(0),
        STREAMING(1),
        LISTENING(2),
        WATCHING(3),
        COMPETING(5)
    }

    enum class StatusDisplayType(val value: Int) {
        NAME(0),
        STATE(1),
        DETAILS(2)
    }

    companion object {
        suspend fun getUserInfo(token: String): Result<UserInfo> = runCatching {
            val client = HttpClient()
            val response = client.get("https://discord.com/api/v9/users/@me") {
                header("Authorization", token)
            }.bodyAsText()
            val json = JSONObject(response)
            val username = json.getString("username")
            val name = json.getString("global_name")
            client.close()

            UserInfo(username, name)
        }
    }
}
