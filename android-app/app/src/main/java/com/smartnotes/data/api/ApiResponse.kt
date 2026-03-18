package com.smartnotes.data.api

import com.google.gson.annotations.SerializedName

/**
 * Unified API response wrapper.
 *
 * All datetime fields in response DTOs (e.g., `createdAt`, `updatedAt`) are received as
 * [String] from the server in ISO-8601 format (e.g., "2025-01-15T10:30:00").
 * Future improvement: add a Gson TypeAdapter to parse them directly into
 * [java.time.LocalDateTime] for type safety at the deserialization boundary.
 */
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
