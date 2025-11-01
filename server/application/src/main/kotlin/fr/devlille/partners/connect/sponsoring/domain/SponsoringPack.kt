package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class SponsoringPack(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,
    @SerialName("max_quantity")
    val maxQuantity: Int?,
    @SerialName("required_options")
    val requiredOptions: List<SponsoringOption>,
    @SerialName("optional_options")
    val optionalOptions: List<SponsoringOption>,
)

/**
 * Polymorphic sealed class hierarchy for sponsoring options.
 *
 * Supports four distinct option types:
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
sealed class SponsoringOption {
    abstract val id: String
    abstract val name: String
    abstract val description: String?
    abstract val price: Int?
}

/**
 * Traditional text-based sponsoring option.
 * Maintains backward compatibility with existing options.
 *
 * JSON representation:
 * {
 *   "type": "text",
 *   "id": "uuid",
 *   "name": "Company description",
 *   "description": "Provide company description",
 *   "price": null
 * }
 */
@Serializable
@SerialName("TEXT")
data class Text(
    override val id: String,
    override val name: String,
    override val description: String?,
    override val price: Int?,
) : SponsoringOption()

/**
 * Quantitative sponsoring option with user-defined quantities.
 * Supports linear pricing: total_price = base_price × selected_quantity
 *
 * Business rules:
 * - quantity must be > 0 when selected
 * - price represents per-unit cost (in cents)
 * - quantitative_descriptor provides unit context (e.g., "job_offer")
 *
 * JSON representation:
 * {
 *   "type": "typed_quantitative",
 *   "id": "uuid",
 *   "name": "Job offers",
 *   "description": "Number of job offers to publish",
 *   "price": 50000,
 *   "quantitative_descriptor": "job_offer"
 * }
 */
@Serializable
@SerialName("TYPED_QUANTITATIVE")
data class TypedQuantitative(
    override val id: String,
    override val name: String,
    override val description: String?,
    override val price: Int?,
    @SerialName("type_descriptor")
    val typeDescriptor: QuantitativeDescriptor,
) : SponsoringOption()

/**
 * Number-based option with fixed quantities.
 * Unlike quantitative options, users cannot modify quantity - it's predefined.
 * Pricing can vary based on business logic (e.g., bulk discounts).
 *
 * Business rules:
 * - fixed_quantity is immutable once set
 * - price represents total cost for the fixed quantity (in cents)
 * - number_descriptor provides context for the fixed amount
 *
 * JSON representation:
 * {
 *   "type": "typed_number",
 *   "id": "uuid",
 *   "name": "Standard booth package",
 *   "description": "Includes booth space and basic amenities",
 *   "price": 150000,
 *   "fixed_quantity": 1,
 *   "number_descriptor": "nb_ticket"
 * }
 */
@Serializable
@SerialName("TYPED_NUMBER")
data class TypedNumber(
    override val id: String,
    override val name: String,
    override val description: String?,
    override val price: Int?,
    @SerialName("type_descriptor")
    val typeDescriptor: NumberDescriptor,
    @SerialName("fixed_quantity")
    val fixedQuantity: Int,
) : SponsoringOption()

/**
 * Selectable option with predefined choices.
 * Users must choose from a limited set of available values.
 * Each selectable value can have its own pricing modifier.
 *
 * Business rules:
 * - selectable_values array must not be empty
 * - selected value must exist in selectable_values
 * - pricing can vary per selected value
 *
 * JSON representation:
 * {
 *   "type": "typed_selectable",
 *   "id": "uuid",
 *   "name": "Booth size",
 *   "description": "Choose your booth configuration",
 *   "price": 100000,
 *   "selectable_descriptor": "booth",
 *   "selectable_values": [
 *     {"id": "small", "name": "Small (3x3m)", "price": 0},
 *     {"id": "large", "name": "Large (6x3m)", "price": 50000}
 *   ]
 * }
 */
@Serializable
@SerialName("TYPED_SELECTABLE")
data class TypedSelectable(
    override val id: String,
    override val name: String,
    override val description: String?,
    override val price: Int?,
    @SerialName("type_descriptor")
    val typeDescriptor: SelectableDescriptor,
    @SerialName("selectable_values")
    val selectableValues: List<SelectableValue>,
) : SponsoringOption()
