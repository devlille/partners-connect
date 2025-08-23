package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.webhooks.domain.WebhookConfig
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object WebhookIntegrationsTable : Table("webhook_integrations") {
    val integrationId = uuid("integration_id").references(IntegrationsTable.id)
    val url = text("url")
    val headerAuth = encryptedVarchar(
        name = "header_auth",
        cipherTextLength = 1000,
        encryptor = SystemVarEnv.Crypto.algorithm,
    ).nullable()
    val type = text("type") // ALL or PARTNERSHIP
    val partnershipId = uuid("partnership_id").nullable()

    override val primaryKey = PrimaryKey(integrationId)
}

operator fun WebhookIntegrationsTable.get(integrationId: UUID): WebhookConfig = transaction {
    WebhookIntegrationsTable
        .selectAll()
        .where { WebhookIntegrationsTable.integrationId eq integrationId }
        .map {
            WebhookConfig(
                url = it[WebhookIntegrationsTable.url],
                headerAuth = it[WebhookIntegrationsTable.headerAuth],
                type = it[WebhookIntegrationsTable.type],
                partnershipId = it[WebhookIntegrationsTable.partnershipId],
            )
        }
        .singleOrNull()
        ?: throw NotFoundException("Webhook config not found")
}
