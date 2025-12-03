package com.my.kizzy.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BatchApiResponse(
    val results: List<ApiResult>
)

@Serializable
data class ApiResult(
    @SerialName("id")
    val id: String,
    @SerialName("original_url")
    val originalUrl: String,
    @SerialName("status")
    val status: String
)
