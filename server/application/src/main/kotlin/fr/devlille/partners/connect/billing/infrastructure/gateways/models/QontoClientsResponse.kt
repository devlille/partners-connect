package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoClientsResponse(
    val clients: List<QontoClient>,
    val meta: QontoMeta,
)

@Serializable
data class QontoMeta(
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("current_page")
    val currentPage: Int,
    @SerialName("next_page")
    val nextPage: Int? = null,
    @SerialName("previous_page")
    val previousPage: Int? = null,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("per_page")
    val perPage: Int,
)
