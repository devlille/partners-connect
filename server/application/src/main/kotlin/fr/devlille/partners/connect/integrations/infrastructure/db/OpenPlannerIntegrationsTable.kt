package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object OpenPlannerIntegrationsTable : Table("openplanner_integrations") {
    val integrationId = uuid("integration_id").references(IntegrationsTable.id)
    val eventId = encryptedVarchar(
        name = "event_id",
        cipherTextLength = 255,
        encryptor = SystemVarEnv.Crypto.algorithm,
    )
    val apiKey = encryptedVarchar(
        name = "api_key",
        cipherTextLength = 255,
        encryptor = SystemVarEnv.Crypto.algorithm,
    )

    override val primaryKey = PrimaryKey(integrationId)
}

data class OpenPlannerConfig(val eventId: String, val apiKey: String)

operator fun OpenPlannerIntegrationsTable.get(integrationId: UUID): OpenPlannerConfig = transaction {
    OpenPlannerIntegrationsTable
        .selectAll()
        .where { OpenPlannerIntegrationsTable.integrationId eq integrationId }
        .map {
            OpenPlannerConfig(
                eventId = it[OpenPlannerIntegrationsTable.eventId],
                apiKey = it[OpenPlannerIntegrationsTable.apiKey],
            )
        }
        .singleOrNull()
        ?: throw NotFoundException("OpenPlanner config not found")
}
