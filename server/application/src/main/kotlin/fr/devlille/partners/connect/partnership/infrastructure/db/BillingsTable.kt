package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object BillingsTable : UUIDTable("billings") {
    val eventId = reference("event_id", EventsTable)
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val name = text("name").nullable()
    val contactFirstName = text("contact_first_name")
    val contactLastName = text("contact_last_name")
    val contactEmail = text("contact_email")
    val po = text("po").nullable()
    val invoicePdfUrl = text("invoice_pdf_url").nullable()
    val quotePdfUrl = text("quote_pdf_url").nullable()
    val status = enumeration<InvoiceStatus>("status")
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
