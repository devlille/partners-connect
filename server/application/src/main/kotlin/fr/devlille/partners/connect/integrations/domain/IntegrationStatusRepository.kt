package fr.devlille.partners.connect.integrations.domain

import java.util.UUID

interface IntegrationStatusRepository {
    suspend fun status(integrationId: UUID): Boolean
}
