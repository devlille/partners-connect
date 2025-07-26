package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

object InvoicesTable : UUIDTable("invoices") {
    val eventId = reference("event_id", EventsTable)
    val companyId = reference("company_id", CompaniesTable)
    val name = text("name").nullable()
    val contactFirstName = text("contact_first_name")
    val contactLastName = text("contact_last_name")
    val contactEmail = text("contact_email")
    val address = text("address")
    val city = text("city")
    val zipCode = text("zip_code")
    val country = varchar("country", 2)
    val siret = text("siret")
    val vat = text("vat")
    val po = text("po").nullable()
    val invoicePdfUrl = text("invoice_pdf_url").nullable()
    val status = enumeration<InvoiceStatus>("status")
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}

enum class InvoiceStatus {
    PENDING,
    SENT,
    PAID,
}
