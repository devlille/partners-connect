package fr.devlille.partners.connect.users.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for revoking user permissions from an organisation.
 *
 * This request model is used to specify which users should have their edit permissions
 * revoked from a specific organisation. The operation is idempotent - revoking permissions
 * for users who don't have permissions will not cause an error.
 *
 * @property userEmails List of email addresses identifying users whose permissions should be revoked
 */
@Serializable
data class RevokePermissionRequest(
    @SerialName("user_emails")
    val userEmails: List<String>,
)
