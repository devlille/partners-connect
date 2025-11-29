package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request data class for updating partnership contact information.
 *
 * All fields are optional to support partial updates using PUT semantics.
 * - Null values mean "don't update this field"
 * - Explicit null can clear optional fields (e.g., phone)
 * - If all fields are null, operation is no-op (returns 200 OK)
 *
 * Validation is performed via JSON schema at API boundary.
 *
 * Example JSON (update all fields):
 * {
 *   "contact_name": "John Doe",
 *   "contact_role": "Developer Relations",
 *   "language": "en",
 *   "phone": "+33123456789",
 *   "emails": ["john.doe@example.com", "contact@example.com"]
 * }
 *
 * Example JSON (partial update - name only):
 * {
 *   "contact_name": "Jane Smith"
 * }
 */
@Serializable
data class UpdatePartnershipContactInfo(
    @SerialName("contact_name")
    val contactName: String? = null,
    @SerialName("contact_role")
    val contactRole: String? = null,
    val language: String? = null,
    val phone: String? = null,
    val emails: List<String>? = null,
)
