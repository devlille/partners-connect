package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.ecosystemPartnerId: UUID
    get() = getValue("ecosystemPartnerId").toUUID()

val StringValues.ecosystemPartnerCategoryId: UUID
    get() = getValue("categoryId").toUUID()
