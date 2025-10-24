package fr.devlille.partners.connect.users.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of a user permission revocation operation.
 *
 * This data class represents the outcome when revoking user permissions from an organisation.
 * It provides information about successfully revoked users and those that were not found.
 *
 * @property revokedCount The number of users whose permissions were successfully revoked (≥ 0)
 * @property notFoundEmails List of email addresses that were not found in the system or had no permissions to revoke
 */
@Serializable
data class RevokeUsersResult(
    @SerialName("revoked_count")
    val revokedCount: Int,
    @SerialName("not_found_emails")
    val notFoundEmails: List<String>,
)
