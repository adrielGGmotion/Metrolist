package com.my.kizzy.remote

import kotlinx.serialization.Serializable

@Serializable
data class ApiResult(
    val id: String,
    val originalUrl: String
)
