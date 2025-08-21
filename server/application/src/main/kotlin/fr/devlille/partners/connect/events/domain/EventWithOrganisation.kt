package fr.devlille.partners.connect.events.domain

import fr.devlille.partners.connect.organisations.domain.OrganisationItem
import kotlinx.serialization.Serializable

@Serializable
data class EventWithOrganisation(
    val event: EventDisplay,
    val organisation: OrganisationItem,
)
