package fr.devlille.partners.connect.events.domain

import fr.devlille.partners.connect.organisations.domain.Organisation
import kotlinx.serialization.Serializable

@Serializable
data class EventWithOrganisation(
    val event: Event,
    val organisation: Organisation,
)
