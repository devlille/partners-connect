package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.sponsoring.domain.NumberDescriptor
import fr.devlille.partners.connect.sponsoring.domain.QuantitativeDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SelectableDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Polymorphic sealed class for partnership-specific options.
 *
 * Includes complete formatted descriptions ready for document generation
 * and pricing details for invoice/quote/agreement workflows.
 *
 * Unlike SponsoringOption (which defines all possible options for a pack),
 * PartnershipOption represents the specific options selected for a partnership
 * with user-chosen values and complete descriptions.
 *
 * Supports four option types:
 * - text: Text-based options (no quantity/value selection)
 * - typed_quantitative: Options with user-selected quantities
 * - typed_number: Options with fixed quantities
 * - typed_selectable: Options with user-selected values
 *
 * Uses @JsonClassDiscriminator("type") for polymorphic JSON serialization.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class PartnershipOption {
    abstract val id: String
    abstract val name: String
    abstract val description: String

    @SerialName("label_with_value")
    abstract val labelWithValue: String
    abstract val price: Int
    abstract val quantity: Int
    abstract val totalPrice: Int

    @SerialName("price_override")
    abstract val priceOverride: Int?
}

/**
 * Text-based partnership option (no quantity or selectable value).
 *
 * Complete description is identical to the original description since there's
 * no quantity or value to append.
 *
 * JSON representation:
 * {
 *   "type": "text",
 *   "id": "uuid",
 *   "name": "Company logo on website",
 *   "description": "Display company logo on event homepage",
 *   "label_with_value": "Display company logo on event homepage",
 *   "price": 10000,
 *   "quantity": 1,
 *   "total_price": 10000
 * }
 */
@Serializable
@SerialName("text")
data class TextPartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("label_with_value")
    override val labelWithValue: String,
    override val price: Int,
    override val quantity: Int = 1,
    @SerialName("total_price")
    override val totalPrice: Int,
    @SerialName("price_override")
    override val priceOverride: Int? = null,
) : PartnershipOption()

/**
 * Quantitative partnership option with user-selected quantity.
 *
 * Complete description appends the selected quantity in parentheses.
 * Format: "{description} ({quantity})"
 *
 * Total price calculated as: price × quantity
 *
 * JSON representation:
 * {
 *   "type": "typed_quantitative",
 *   "id": "uuid",
 *   "name": "Job offers",
 *   "description": "Publish job offers on event website",
 *   "label_with_value": "Publish job offers on event website (3)",
 *   "price": 50000,
 *   "quantity": 3,
 *   "total_price": 150000,
 *   "type_descriptor": "job_offer"
 * }
 */
@Serializable
@SerialName("typed_quantitative")
data class QuantitativePartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("label_with_value")
    override val labelWithValue: String,
    override val price: Int,
    override val quantity: Int,
    @SerialName("total_price")
    override val totalPrice: Int,
    @SerialName("type_descriptor")
    val typeDescriptor: QuantitativeDescriptor,
    @SerialName("price_override")
    override val priceOverride: Int? = null,
) : PartnershipOption()

/**
 * Number-based partnership option with fixed quantity.
 *
 * Complete description appends the fixed quantity in parentheses.
 * Format: "{description} ({fixedQuantity})"
 *
 * Total price calculated as: price × fixedQuantity
 *
 * JSON representation:
 * {
 *   "type": "typed_number",
 *   "id": "uuid",
 *   "name": "Conference passes",
 *   "description": "Access passes for conference attendees",
 *   "label_with_value": "Access passes for conference attendees (10)",
 *   "price": 10000,
 *   "quantity": 10,
 *   "total_price": 100000,
 *   "type_descriptor": "nb_ticket"
 * }
 */
@Serializable
@SerialName("typed_number")
data class NumberPartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("label_with_value")
    override val labelWithValue: String,
    override val price: Int,
    override val quantity: Int,
    @SerialName("total_price")
    override val totalPrice: Int,
    @SerialName("type_descriptor")
    val typeDescriptor: NumberDescriptor,
    @SerialName("price_override")
    override val priceOverride: Int? = null,
) : PartnershipOption()

/**
 * Selectable partnership option with user-chosen value.
 *
 * Complete description appends the selected value name in parentheses.
 * Format: "{description} ({selectedValue.value})"
 *
 * Total price uses the selected value's price (quantity always 1).
 *
 * JSON representation:
 * {
 *   "type": "typed_selectable",
 *   "id": "uuid",
 *   "name": "Exhibition booth location",
 *   "description": "Choose your booth location in the exhibition hall",
 *   "label_with_value": "Choose your booth location in the exhibition hall (Stand A1 - Corner
booth near entrance)",
 *   "price": 30000,
 *   "quantity": 1,
 *   "total_price": 30000,
 *   "type_descriptor": "booth",
 *   "selected_value": {
 *     "id": "stand-a1",
 *     "value": "Stand A1 - Corner booth near entrance",
 *     "price": 30000
 *   }
 * }
 */
@Serializable
@SerialName("typed_selectable")
data class SelectablePartnershipOption(
    override val id: String,
    override val name: String,
    override val description: String,
    @SerialName("label_with_value")
    override val labelWithValue: String,
    override val price: Int,
    override val quantity: Int = 1,
    @SerialName("total_price")
    override val totalPrice: Int,
    @SerialName("type_descriptor")
    val typeDescriptor: SelectableDescriptor,
    @SerialName("selected_value")
    val selectedValue: SelectedValue,
    @SerialName("price_override")
    override val priceOverride: Int? = null,
) : PartnershipOption()

/**
 * Selected value for selectable partnership options.
 *
 * Represents the specific choice made by the partner from available selectable values.
 */
@Serializable
data class SelectedValue(
    val id: String,
    val value: String,
    val price: Int,
)
