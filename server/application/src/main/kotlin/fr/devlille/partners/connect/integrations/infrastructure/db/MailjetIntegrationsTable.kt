package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object MailjetIntegrationsTable : Table("mailjet_integrations") {
    val integrationId = uuid("integration_id").references(IntegrationsTable.id)
    val apiKey = encryptedVarchar(
        name = "api_key",
        cipherTextLength = 255,
        encryptor = SystemVarEnv.Crypto.algorithm,
    )
    val secret = encryptedVarchar(
        name = "secret",
        cipherTextLength = 255,
        encryptor = SystemVarEnv.Crypto.algorithm,
    )

    override val primaryKey = PrimaryKey(integrationId)
}

data class MailjetConfig(val apiKey: String, val secret: String)

operator fun MailjetIntegrationsTable.get(integrationId: UUID): MailjetConfig = transaction {
    MailjetIntegrationsTable
        .selectAll()
        .where { MailjetIntegrationsTable.integrationId eq integrationId }
        .map {
            MailjetConfig(
                apiKey = it[MailjetIntegrationsTable.apiKey],
                secret = it[MailjetIntegrationsTable.secret],
            )
        }
        .singleOrNull()
        ?: throw NotFoundException(
            code = ErrorCode.INTEGRATION_NOT_FOUND,
            message = "Mailjet config not found",
            meta = mapOf(MetaKeys.ID to integrationId.toString()),
        )
}
