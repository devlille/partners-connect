package fr.devlille.partners.connect.agenda.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.speakerId: UUID
    get() = getValue("speakerId").toUUID()
