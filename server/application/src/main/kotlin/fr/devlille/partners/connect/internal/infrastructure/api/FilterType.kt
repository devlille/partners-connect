package fr.devlille.partners.connect.internal.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FilterType {
    @SerialName("string")
    STRING,

    @SerialName("boolean")
    BOOLEAN,
}
