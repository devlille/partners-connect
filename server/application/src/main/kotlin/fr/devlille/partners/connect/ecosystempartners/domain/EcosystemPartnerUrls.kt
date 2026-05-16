package fr.devlille.partners.connect.ecosystempartners.domain

import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv

fun publicEventUrl(event: EventWithOrganisation): String =
    "${SystemVarEnv.frontendBaseUrl}/${event.event.slug}"
