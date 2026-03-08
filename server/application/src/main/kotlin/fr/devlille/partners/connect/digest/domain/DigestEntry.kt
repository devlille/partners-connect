package fr.devlille.partners.connect.digest.domain

/**
 * A single actionable partnership entry to display in one digest section.
 *
 * @param companyName The display name of the sponsor company.
 * @param partnershipLink The full URL linking directly to the partnership record in the platform.
 */
data class DigestEntry(
    val companyName: String,
    val partnershipLink: String,
)
