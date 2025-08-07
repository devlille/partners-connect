package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.crypt.encryptedVarchar
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object BilletWebIntegrationsTable : Table("billetweb_integrations") {
    val integrationId = uuid("integration_id").references(IntegrationsTable.id)
    val basic = encryptedVarchar(
        name = "basic",
        cipherTextLength = 255,
        encryptor = SystemVarEnv.Crypto.algorithm,
    )
    val eventId = varchar("event_id", length = 50)
    val rateId = varchar("rate_id", length = 50)

    override val primaryKey = PrimaryKey(integrationId)
}

data class BilletWebConfig(val basic: String, val eventId: String, val rateId: String)

operator fun BilletWebIntegrationsTable.get(integrationId: UUID): BilletWebConfig = transaction {
    BilletWebIntegrationsTable
        .selectAll()
        .where { BilletWebIntegrationsTable.integrationId eq integrationId }
        .map {
            BilletWebConfig(
                basic = it[BilletWebIntegrationsTable.basic],
                eventId = it[BilletWebIntegrationsTable.eventId],
                rateId = it[BilletWebIntegrationsTable.rateId],
            )
        }
        .singleOrNull()
        ?: throw NotFoundException("BilletWeb config not found")
}
