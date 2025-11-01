package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request data class for suggesting partnership changes with enhanced option selections.
 *
 * Updated to match RegisterPartnership structure using polymorphic option_selections
 * instead of simple option_ids.
 *
 * Supports all four option selection types:
 * - text_selection: Text input for traditional options
 * - quantitative_selection: User-defined quantity (price calculated on backend)
 * - number_selection: Fixed-quantity options (presence means selected)
 * - selectable_selection: Choice from predefined values (price calculated on backend)
 *
 * Example JSON:
 * {
 *   "pack_id": "uuid",
 *   "language": "en",
 *   "option_selections": [
 *     {
 *       "type": "quantitative_selection",
 *       "option_id": "uuid",
 *       "selected_quantity": 10
 *     },
 *     {
 *       "type": "selectable_selection",
 *       "option_id": "uuid",
 *       "selected_value": "medium"
 *     }
 *   ]
 * }
 */
@Serializable
data class SuggestPartnership(
    @SerialName("pack_id")
    val packId: String,
    val language: String,
    @SerialName("option_selections")
    val optionSelections: List<PartnershipOptionSelection> = emptyList(),
)
