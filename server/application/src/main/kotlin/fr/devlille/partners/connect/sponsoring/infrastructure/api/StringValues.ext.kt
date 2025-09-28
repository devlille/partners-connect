package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.packId: UUID
    get() = getValue("packId").toUUID()

val StringValues.optionId: UUID
    get() = getValue("optionId").toUUID()
