package fr.devlille.partners.connect.billing.infrastructure.gateways.models

import kotlinx.serialization.Serializable

@Serializable
data class QontoClientResponse(
    val client: QontoClient,
)
