package com.smartnotes.data.api

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
) {
    val isSuccess: Boolean get() = code == 0
}

data class PageResponse<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("first") val first: Boolean,
    @SerializedName("last") val last: Boolean
)
