package fr.devlille.partners.connect.internal.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DEFAULT_PAGE_SIZE = 20

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Long,
)
