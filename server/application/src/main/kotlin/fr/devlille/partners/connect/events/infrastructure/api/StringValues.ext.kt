package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.eventSlug: String
    get() = getValue("eventSlug")

val StringValues.linkUUID: UUID
    get() = getValue("linkId").toUUID()
