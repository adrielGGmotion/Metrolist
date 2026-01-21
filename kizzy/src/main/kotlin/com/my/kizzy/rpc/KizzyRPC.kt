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
    
    // Rate limiting to prevent Discord from flagging the account
    private var lastUpdateTime = 0L
    private var lastActivityHash = 0

    fun closeRPC() {
        discordWebSocket.close()
    }

    fun isRpcRunning(): Boolean {
        return discordWebSocket.isWebSocketConnected()
    }

    open suspend fun close() {
        if (!isRpcRunning()) {
            discordWebSocket.connect()
        }
        val presence = Presence(
            activities = emptyList()
        )
        discordWebSocket.sendActivity(presence)
    }

    suspend fun setActivity(
        name: String,
        state: String?,
        stateUrl: String? = null,
        details: String?,
        detailsUrl: String? = null,
        largeImage: RpcImage?,
        smallImage: RpcImage?,
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
        forceUpdate: Boolean = false,
    ) {
        if (!isRpcRunning()) {
            discordWebSocket.connect()
        }
        
        // Create a hash of the activity data to detect meaningful changes
        val activityHash = listOf(
            name, state, details, type.value, statusDisplayType.value,
            largeImage?.toString(), smallImage?.toString()
        ).hashCode()
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastUpdateTime
        
        // Rate limiting: Enforce minimum interval and prevent redundant updates
        val shouldUpdate = when {
            forceUpdate -> true // Periodic updates always go through
            timeSinceLastUpdate < MIN_UPDATE_INTERVAL_MS -> false // Too soon
            activityHash != lastActivityHash -> true // Activity changed
            timeSinceLastUpdate > MAX_STALE_TIME_MS -> true // Force refresh if too old
            else -> false
        }
        
        if (!shouldUpdate) {
            return
        }
        
        // Resolve external images with enhanced fallback support
        val images = listOfNotNull(largeImage, smallImage)
        val externalImages = images.filterIsInstance<RpcImage.ExternalImage>()
        val imageUrls = externalImages.map { it.image }
        val resolvedImages = kizzyRepository.getImages(imageUrls)?.results?.associate { it.originalUrl to it.id } ?: emptyMap()

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
                    assets = Assets(
                        largeImage = largeImage?.let { 
                            when (it) {
                                is RpcImage.DiscordImage -> "mp:${it.image}"
                                is RpcImage.ExternalImage -> {
                                    val resolvedId = resolvedImages[it.image]
                                    // Ensure we have a valid ID (not null or empty)
                                    // Worker returns fallback IDs even on error status
                                    if (!resolvedId.isNullOrEmpty()) resolvedId else null
                                }
                            }
                        },
                        smallImage = smallImage?.let { 
                            when (it) {
                                is RpcImage.DiscordImage -> "mp:${it.image}"
                                is RpcImage.ExternalImage -> {
                                    val resolvedId = resolvedImages[it.image]
                                    // Ensure we have a valid ID (not null or empty)
                                    if (!resolvedId.isNullOrEmpty()) resolvedId else null
                                }
                            }
                        },
                        largeText = largeText,
                        smallText = smallText
                    ),
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
        discordWebSocket.sendActivity(presence)
        
        // Update rate limiting tracking
        lastUpdateTime = currentTime
        lastActivityHash = activityHash
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
        private const val MIN_UPDATE_INTERVAL_MS = 10_000L // 10 seconds minimum between updates
        private const val MAX_STALE_TIME_MS = 30_000L // Force update after 30s even if no change
        
        suspend fun getUserInfo(token: String): Result<UserInfo> = runCatching {
            val client = HttpClient()
            val response = client.get("https://discord.com/api/v10/users/@me") {
                header("Authorization", token)
            }.bodyAsText()
            val json = JSONObject(response)
            val username = json.getString("username")
            val name = json.optString("global_name", username)
            client.close()

            UserInfo(username, name)
        }
    }
}
