package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object PartnershipsTable : UUIDTable("partnerships") {
    val eventId = uuid("event_id").references(EventsTable.id)
    val companyId = uuid("company_id").references(CompaniesTable.id)
    val phone = text("phone").nullable()
    val language = text("language")
    val selectedPackId = uuid("selected_pack_id").references(SponsoringPacksTable.id).nullable()
    val suggestionPackId = uuid("suggestion_pack_id").references(SponsoringPacksTable.id).nullable()
    val suggestionSentAt = datetime("suggestion_sent_at").nullable()
    val approvedAt = datetime("approved_at").nullable()
    val declinedAt = datetime("declined_at").nullable()
    val validatedAt = datetime("validated_at").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
