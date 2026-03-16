package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlanEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object MigratePartnershipCommunicationsMigration : Migration {
    override val id = "20260316_migrate_partnership_communications"
    override val description = "Migrate communication dates/urls from partnerships to communication_plans"

    override fun up() {
        transaction {
            PartnershipEntity
                .find { PartnershipsTable.communicationPublicationDate.isNotNull() }
                .forEach { partnership ->
                    CommunicationPlanEntity.new(UUID.randomUUID()) {
                        event = partnership.event
                        this.partnership = partnership
                        title = partnership.company.name
                        scheduledDate = partnership.communicationPublicationDate
                        supportUrl = partnership.communicationSupportUrl
                    }
                }
        }
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported — would cause data loss",
        )
    }
}
