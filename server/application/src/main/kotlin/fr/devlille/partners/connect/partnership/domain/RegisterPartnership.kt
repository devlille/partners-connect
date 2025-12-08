package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request data class for creating partnerships with enhanced option selections.
 *
 * Breaking change from simple option_ids to polymorphic option_selections.
 * Supports all four option selection types:
 * - text_selection: Text input for traditional options
 * - quantitative_selection: User-defined quantity (price calculated on backend)
 * - number_selection: Fixed-quantity options (presence means selected)
 * - selectable_selection: Choice from predefined values (price calculated on backend)
 *
 * Example JSON:
 * {
 *   "company_id": "uuid",
 *   "pack_id": "uuid",
 *   "contact_name": "John Doe",
 *   "contact_role": "Developer",
 *   "phone": "+33123456789",
 *   "language": "en",
 *   "option_selections": [
 *     {
 *       "type": "quantitative_selection",
 *       "option_id": "uuid",
 *       "selected_quantity": 25
 *     },
 *     {
 *       "type": "selectable_selection",
 *       "option_id": "uuid",
 *       "selected_value": "large"
 *     }
 *   ]
 * }
 */
@Serializable
data class RegisterPartnership(
    @SerialName("company_id")
    val companyId: String,
    @SerialName("pack_id")
    val packId: String,
    val language: String,
    @SerialName("contact_name")
    val contactName: String? = null,
    @SerialName("contact_role")
    val contactRole: String? = null,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    @SerialName("option_selections")
    val optionSelections: List<PartnershipOptionSelection> = emptyList(),
)
