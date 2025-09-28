package fr.devlille.partners.connect.organisations.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import io.ktor.util.StringValues

val StringValues.orgSlug: String
    get() = getValue("orgSlug")
