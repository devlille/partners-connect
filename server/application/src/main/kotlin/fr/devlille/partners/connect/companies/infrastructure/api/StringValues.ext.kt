package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.companyUUID: UUID
    get() = getValue("companyId").toUUID()
