package fr.devlille.partners.connect.events.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Long,
)
