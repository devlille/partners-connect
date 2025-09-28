package fr.devlille.partners.connect.internal.infrastructure.api

import kotlinx.serialization.Serializable

@Serializable
class ResponseException(
    val message: String,
    val errors: List<String> = emptyList(),
    val stack: String? = null,
)
