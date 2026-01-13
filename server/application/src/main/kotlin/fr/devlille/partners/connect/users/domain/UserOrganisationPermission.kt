package fr.devlille.partners.connect.users.domain

import fr.devlille.partners.connect.organisations.domain.Organisation
import kotlinx.serialization.Serializable

@Serializable
data class UserOrganisationPermission(
    val userId: String,
    val user: User,
    val organisation: Organisation,
    val canEdit: Boolean,
)
