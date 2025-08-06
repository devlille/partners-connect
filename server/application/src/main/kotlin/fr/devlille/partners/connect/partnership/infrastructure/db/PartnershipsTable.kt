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
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable)
    val phone = text("phone").nullable()
    val contactName = text("contact_name")
    val contactRole = text("contact_role")
    val language = text("language")
    val agreementUrl = text("agreement_url").nullable()
    val agreementSignedUrl = text("agreement_signed_url").nullable()
    val selectedPackId = reference("selected_pack_id", SponsoringPacksTable).nullable()
    val suggestionPackId = reference("suggestion_pack_id", SponsoringPacksTable).nullable()
    val suggestionSentAt = datetime("suggestion_sent_at").nullable()
    val suggestionApprovedAt = datetime("suggestion_approved_at").nullable()
    val suggestionDeclinedAt = datetime("suggestion_declined_at").nullable()
    val declinedAt = datetime("declined_at").nullable()
    val validatedAt = datetime("validated_at").nullable()
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
