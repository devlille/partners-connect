package fr.devlille.partners.connect.internal.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FilterValue(
    val value: String,
    @SerialName("display_value")
    val displayValue: String,
)
