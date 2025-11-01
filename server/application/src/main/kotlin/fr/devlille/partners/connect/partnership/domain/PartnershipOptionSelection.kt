package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Polymorphic sealed class hierarchy for partnership option selections.
 *
 * Represents user selections for different sponsoring option types:
 * - text_selection: Text input for traditional options
 * - quantitative_selection: User-defined quantity (price calculated on backend)
 * - number_selection: Selection of fixed-quantity options (presence means selected)
 * - selectable_selection: Choice from predefined values (price calculated on backend)
 *
 * Uses @JsonClassDiscriminator("type") for polymorphic JSON serialization/deserialization.
 * All subtypes use @SerialName with snake_case for API consistency.
 *
 * Note: calculatedPrice is NOT included here - it's computed on the backend side.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed class PartnershipOptionSelection {
    abstract val optionId: String
}

/**
 * Text-based option selection.
 * Maintains backward compatibility with existing text options.
 *
 * JSON representation:
 * {
 *   "type": "text_selection",
 *   "option_id": "uuid"
 * }
 */
@Serializable
@SerialName("text_selection")
data class TextSelection(
    @SerialName("option_id")
    override val optionId: String,
) : PartnershipOptionSelection()

/**
 * Quantitative option selection with user-defined quantity.
 * Price is calculated on the backend: calculated_price = base_price × selected_quantity
 *
 * Business rules:
 * - selected_quantity must be > 0
 * - quantity represents user's desired amount
 *
 * JSON representation:
 * {
 *   "type": "quantitative_selection",
 *   "option_id": "uuid",
 *   "selected_quantity": 25
 * }
 */
@Serializable
@SerialName("quantitative_selection")
data class QuantitativeSelection(
    @SerialName("option_id")
    override val optionId: String,
    @SerialName("selected_quantity")
    val selectedQuantity: Int,
) : PartnershipOptionSelection()

/**
 * Number option selection for fixed-quantity options.
 * Presence in the selections list indicates the option is selected.
 *
 * Business rules:
 * - No additional fields needed - presence means selected
 * - Price is calculated on backend using option.price
 * - fixed_quantity is read-only from the option definition
 *
 * JSON representation:
 * {
 *   "type": "number_selection",
 *   "option_id": "uuid"
 * }
 */
@Serializable
@SerialName("number_selection")
data class NumberSelection(
    @SerialName("option_id")
    override val optionId: String,
) : PartnershipOptionSelection()

/**
 * Selectable option selection from predefined values.
 * User chooses one value from available options by its ID.
 *
 * Business rules:
 * - selected_value_id must exist in option.selectable_values (by id)
 * - Price is calculated on backend using the specific selected value's price
 * - Only one value can be selected per option
 * - Uses ID-based selection for multi-language support
 *
 * JSON representation:
 * {
 *   "type": "selectable_selection",
 *   "option_id": "uuid",
 *   "selected_value_id": "value-uuid"
 * }
 */
@Serializable
@SerialName("selectable_selection")
data class SelectableSelection(
    @SerialName("option_id")
    override val optionId: String,
    @SerialName("selected_value_id")
    val selectedValueId: String,
) : PartnershipOptionSelection()
