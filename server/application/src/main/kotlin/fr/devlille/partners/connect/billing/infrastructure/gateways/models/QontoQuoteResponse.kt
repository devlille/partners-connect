package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QontoQuoteResponse(
    val id: String,
    val number: String,
    @SerialName("quote_url")
    val quoteUrl: String,
    @SerialName("created_at")
    val createdAt: String,
)
