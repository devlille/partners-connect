package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Polymorphic sealed class hierarchy for creating sponsoring options.
 *
 * Supports four distinct option types for creation:
 * - text: Traditional text-based options (backward compatible)
 * - typed_quantitative: Options with user-defined quantities and linear pricing
 * - typed_number: Options with fixed quantities but variable unit pricing
 * - typed_selectable: Options with predefined selectable values
 *
 * Uses @JsonClassDiscriminator("type") for polymorphic JSON serialization/deserialization.
 * All subtypes use @SerialName with snake_case for API consistency.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class CreateSponsoringOption {
    abstract val translations: List<TranslatedLabel>
    abstract val price: Int?
}

/**
 * Traditional text-based sponsoring option creation.
 * Maintains backward compatibility with existing options.
 *
 * JSON representation:
 * {
 *   "type": "text",
 *   "translations": [...],
 *   "price": null
 * }
 */
@Serializable
@SerialName("text")
data class CreateText(
    override val translations: List<TranslatedLabel>,
    override val price: Int? = null,
) : CreateSponsoringOption()

/**
 * Quantitative sponsoring option creation with user-defined quantities.
 * Supports linear pricing: total_price = base_price × selected_quantity
 *
 * JSON representation:
 * {
 *   "type": "typed_quantitative",
 *   "translations": [...],
 *   "price": 50000,
 *   "type_descriptor": "job_offer"
 * }
 */
@Serializable
@SerialName("typed_quantitative")
data class CreateTypedQuantitative(
    override val translations: List<TranslatedLabel>,
    override val price: Int? = null,
    @SerialName("type_descriptor")
    val typeDescriptor: QuantitativeDescriptor,
) : CreateSponsoringOption()

/**
 * Number-based option creation with fixed quantities.
 * Unlike quantitative options, users cannot modify quantity - it's predefined.
 *
 * JSON representation:
 * {
 *   "type": "typed_number",
 *   "translations": [...],
 *   "price": 150000,
 *   "type_descriptor": "nb_ticket",
 *   "fixed_quantity": 1
 * }
 */
@Serializable
@SerialName("typed_number")
data class CreateTypedNumber(
    override val translations: List<TranslatedLabel>,
    override val price: Int? = null,
    @SerialName("type_descriptor")
    val typeDescriptor: NumberDescriptor,
    @SerialName("fixed_quantity")
    val fixedQuantity: Int,
) : CreateSponsoringOption()

/**
 * Selectable option creation with predefined choices and individual pricing.
 * Users must choose from a limited set of available values, each with its own price.
 *
 * Note: The root price is typically null as individual values have their own pricing.
 *
 * JSON representation:
 * {
 *   "type": "typed_selectable",
 *   "translations": [...],
 *   "price": null,
 *   "type_descriptor": "booth",
 *   "selectable_values": [
 *     {"value": "small", "price": 100000},
 *     {"value": "large", "price": 150000}
 *   ]
 * }
 */
@Serializable
@SerialName("typed_selectable")
data class CreateTypedSelectable(
    override val translations: List<TranslatedLabel>,
    override val price: Int? = null,
    @SerialName("type_descriptor")
    val typeDescriptor: SelectableDescriptor,
    @SerialName("selectable_values")
    val selectableValues: List<CreateSelectableValue>,
) : CreateSponsoringOption()

@Serializable
data class TranslatedLabel(
    val language: String,
    val name: String,
    val description: String? = null,
)
