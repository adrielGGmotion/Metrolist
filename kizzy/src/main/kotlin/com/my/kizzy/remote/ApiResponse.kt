package com.my.kizzy.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BatchApiResponse(
    @SerialName("results")
    val results: List<ApiResult>,
)

@Serializable
data class ApiResult(
    @SerialName("original_url")
    val originalUrl: String,
    @SerialName("status")
    val status: String,
    @SerialName("id")
    val id: String,
)
