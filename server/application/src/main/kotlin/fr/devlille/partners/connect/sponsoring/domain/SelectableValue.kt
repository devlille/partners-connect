package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

/**
 * Represents a selectable value with its ID, display value, and individual price.
 *
 * Used in TypedSelectable options to provide pricing per selection choice.
 * Prepares for multi-language support by using ID-based references.
 */
@Serializable
data class SelectableValue(
    val id: String,
    val value: String,
    val price: Int,
)
