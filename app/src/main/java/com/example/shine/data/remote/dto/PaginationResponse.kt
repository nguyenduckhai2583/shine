package com.example.shine.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic pagination wrapper for API responses.
 *
 * Can be reused across any endpoint returning paginated data:
 * ```
 * @GET("path/to/pagination")
 * suspend fun getItems(...): PaginationResponse<ItemDto>
 * ```
 */
@Serializable
data class PaginationResponse<T>(
    @SerialName("items")
    val items: List<T>? = null,
    @SerialName("total")
    val total: Int = 0,
    @SerialName("lastPage")
    val lastPage: Int = 0,
    @SerialName("perPage")
    val perPage: Int = 0,
    @SerialName("currentPage")
    val currentPage: Int = 0,
) {
    /**
     * Non-null list of items.
     */
    val itemList: List<T>
        get() = items.orEmpty()

    /**
     * Indicates whether a next page is available.
     */
    val hasNextPage: Boolean
        get() = if (lastPage > 0) {
            currentPage < lastPage
        } else if (perPage > 0) {
            currentPage * perPage < total
        } else {
            false
        }

    /**
     * Maps the items to a new type while preserving pagination metadata.
     */
    inline fun <R> map(transform: (T) -> R): PaginationResponse<R> {
        return PaginationResponse(
            items = items?.map(transform),
            total = total,
            lastPage = lastPage,
            perPage = perPage,
            currentPage = currentPage,
        )
    }
}
