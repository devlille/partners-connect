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

object QontoIntegrationsTable : Table("qonto_integrations") {
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
    val sandboxToken = encryptedVarchar(
        name = "sandbox_token",
        cipherTextLength = 255,
        encryptor = SystemVarEnv.Crypto.algorithm,
    )

    override val primaryKey = PrimaryKey(integrationId)
}

data class QontoConfig(val apiKey: String, val secret: String, val sandboxToken: String)

operator fun QontoIntegrationsTable.get(integrationId: UUID): QontoConfig = transaction {
    QontoIntegrationsTable
        .selectAll()
        .where { QontoIntegrationsTable.integrationId eq integrationId }
        .map {
            QontoConfig(
                apiKey = it[QontoIntegrationsTable.apiKey],
                secret = it[QontoIntegrationsTable.secret],
                sandboxToken = it[QontoIntegrationsTable.sandboxToken],
            )
        }
        .singleOrNull()
        ?: throw NotFoundException(
            code = ErrorCode.INTEGRATION_NOT_FOUND,
            message = "Qonto config not found",
            meta = mapOf(MetaKeys.ID to integrationId.toString()),
        )
}
