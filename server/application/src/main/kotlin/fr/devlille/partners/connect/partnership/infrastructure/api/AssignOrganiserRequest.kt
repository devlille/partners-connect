package fr.devlille.partners.connect.partnership.infrastructure.api

import kotlinx.serialization.Serializable

/**
 * Request to assign an organiser to a partnership.
 *
 * @property email Email address of the user to assign as organiser
 */
@Serializable
data class AssignOrganiserRequest(
    val email: String,
)
