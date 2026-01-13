package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailHistoryTable
import fr.devlille.partners.connect.partnership.infrastructure.db.RecipientDeliveryStatusTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add partnership email history tracking.
 * Creates two new tables:
 * - partnership_email_history: Stores all emails sent to partnerships
 * - recipient_delivery_status: Stores per-recipient delivery status for each email
 */
object AddPartnershipEmailHistoryMigration : Migration {
    override val id = "20260110_add_partnership_email_history"
    override val description = "Add partnership_email_history and recipient_delivery_status tables"

    override fun up() {
        SchemaUtils.create(
            PartnershipEmailHistoryTable,
            RecipientDeliveryStatusTable,
        )
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported - email history is immutable audit trail, dropping tables would cause data loss",
        )
    }
}
