package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.server.plugins.ParameterConversionException
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.usage: IntegrationUsage
    get() {
        val usageParam = getValue("usage")
        return runCatching { IntegrationUsage.valueOf(usageParam.uppercase()) }
            .getOrElse {
                throw ParameterConversionException(parameterName = "usage", type = "IntegrationUsage", cause = it)
            }
    }

val StringValues.provider: IntegrationProvider
    get() {
        val providerParam = getValue("provider")
        return runCatching { IntegrationProvider.valueOf(providerParam.uppercase()) }
            .getOrElse {
                throw ParameterConversionException(parameterName = "provider", type = "IntegrationProvider", cause = it)
            }
    }

val StringValues.integrationId: UUID
    get() = getValue("integrationId").toUUID()
