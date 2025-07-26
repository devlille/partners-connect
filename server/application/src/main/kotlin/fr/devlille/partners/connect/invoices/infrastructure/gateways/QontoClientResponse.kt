package fr.devlille.partners.connect.invoices.infrastructure.gateways

import kotlinx.serialization.Serializable

@Serializable
data class QontoClientResponse(
    val client: QontoClient,
)
