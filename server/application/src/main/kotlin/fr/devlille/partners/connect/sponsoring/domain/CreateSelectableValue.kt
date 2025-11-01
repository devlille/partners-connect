package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

/**
 * Data class for creating selectable values with individual pricing.
 * Used in CreateTypedSelectable to specify both the value and its price.
 *
 * This enables individual pricing per selectable option and prepares for
 * multi-language support where values are referenced by ID.
 *
 * JSON representation:
 * {
 *   "value": "3x3m",
 *   "price": 150000
 * }
 */
@Serializable
data class CreateSelectableValue(
    val value: String,
    val price: Int,
)
