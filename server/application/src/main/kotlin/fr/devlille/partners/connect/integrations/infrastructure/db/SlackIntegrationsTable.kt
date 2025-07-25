package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object SlackIntegrationsTable : Table("slack_integrations") {
    val integrationId = uuid("integration_id").references(IntegrationsTable.id)
    val token = encryptedVarchar(
        name = "token",
        cipherTextLength = 255,
        encryptor = SystemVarEnv.Crypto.algorithm,
    )
    val channel = text("channel")

    override val primaryKey = PrimaryKey(integrationId)
}

data class SlackConfig(val token: String, val channel: String)

operator fun SlackIntegrationsTable.get(integrationId: UUID): SlackConfig = transaction {
    SlackIntegrationsTable
        .selectAll()
        .where { SlackIntegrationsTable.integrationId eq integrationId }
        .map {
            SlackConfig(
                token = it[SlackIntegrationsTable.token],
                channel = it[SlackIntegrationsTable.channel],
            )
        }
        .singleOrNull()
        ?: throw NotFoundException("Slack config not found")
}
