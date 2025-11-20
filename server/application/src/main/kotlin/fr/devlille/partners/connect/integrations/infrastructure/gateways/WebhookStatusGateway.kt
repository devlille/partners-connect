package fr.devlille.partners.connect.integrations.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class WebhookStatusGateway(
    private val httpClient: HttpClient,
) : StatusGateway {
    override val provider: IntegrationProvider
        get() = IntegrationProvider.WEBHOOK

    override suspend fun status(integrationId: UUID): Boolean {
        val config = transaction { WebhookIntegrationsTable[integrationId] }
        if (config.healthUrl == null) {
            throw NotFoundException("Health url not set")
        }
        try {
            val response = httpClient.get(config.healthUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    config.headerAuth?.let { auth ->
                        append(HttpHeaders.Authorization, auth)
                    }
                }
            }
            return response.status.isSuccess()
        } catch (_: UnauthorizedException) {
            return false
        }
    }
}
