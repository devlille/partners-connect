package fr.devlille.partners.connect.internal.infrastructure.api

import kotlinx.serialization.Serializable

@Serializable
data class FilterDefinition(
    val name: String,
    val type: FilterType,
    val values: List<FilterValue>? = null,
)
