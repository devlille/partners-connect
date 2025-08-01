package fr.devlille.partners.connect.users.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GrantPermissionRequest(
    @SerialName("user_emails")
    val userEmails: List<String>,
)
