package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.crypt.encryptedVarchar

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
