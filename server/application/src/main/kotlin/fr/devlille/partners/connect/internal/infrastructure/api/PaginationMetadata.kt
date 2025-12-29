package fr.devlille.partners.connect.internal.infrastructure.api

import kotlinx.serialization.Serializable

@Serializable
data class PaginationMetadata(
    val filters: List<FilterDefinition>,
    val sorts: List<String>,
)
