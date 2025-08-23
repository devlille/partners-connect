package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.integrations.domain.WebhookType
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

data class WebhookConfig(
    val url: String,
    val headerAuth: String?,
    val type: WebhookType,
    val partnershipId: UUID?,
)

object WebhookIntegrationsTable : Table("webhook_integrations") {
    val integrationId = uuid("integration_id").references(IntegrationsTable.id)
    val url = text("url")
    val headerAuth = encryptedVarchar(
        name = "header_auth",
        cipherTextLength = 1000,
        encryptor = SystemVarEnv.Crypto.algorithm,
    ).nullable()
    val type = enumeration<WebhookType>("type")
    val partnershipId = reference("partnership_id", PartnershipsTable).nullable()

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
                partnershipId = it[WebhookIntegrationsTable.partnershipId]?.value,
            )
        }
        .singleOrNull()
        ?: throw NotFoundException("Webhook config not found")
}
