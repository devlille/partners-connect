package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.pdf.renderMarkdownToPdf
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import fr.devlille.partners.connect.internal.infrastructure.templating.templating
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.partnership.domain.Company
import fr.devlille.partners.connect.partnership.domain.ContactInfo
import fr.devlille.partners.connect.partnership.domain.Event
import fr.devlille.partners.connect.partnership.domain.Organisation
import fr.devlille.partners.connect.partnership.domain.PartnershipAgreement
import fr.devlille.partners.connect.partnership.domain.PartnershipAgreementRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipInfo
import fr.devlille.partners.connect.partnership.domain.PartnershipPricing
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.hasBoothFromOptions
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.time.Duration.Companion.days

class PartnershipAgreementRepositoryExposed : PartnershipAgreementRepository {
    @OptIn(FormatStringsInDatetimeFormats::class)
    override fun agreement(eventSlug: String, partnershipId: UUID): PartnershipAgreement = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership not found")
        val pack = partnership.validatedPack()
            ?: throw NotFoundException("Validated pack not found for partnership")
        val formatter = LocalDate.Format { byUnicodePattern("yyyy/MM/dd") }
        PartnershipAgreement(
            path = "/agreement/${partnership.language}.md",
            organisation = event.organisation.toAgreementOrganisation(formatter),
            event = event.toAgreementEvent(formatter),
            company = partnership.company.toAgreementCompany(),
            partnership = partnership.toAgreementPartnership(pack),
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.format(formatter),
            location = "Lille, France",
        )
    }

    override fun generatePDF(
        agreement: PartnershipAgreement,
        pricing: PartnershipPricing,
    ): ByteArray {
        val template = readResourceFile(agreement.path)
        val markdown = templating(template, AgreementScope(agreement, pricing))
        return renderMarkdownToPdf(markdown)
    }

    override fun updateAgreementUrl(eventSlug: String, partnershipId: UUID, agreementUrl: String): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.agreementUrl = agreementUrl
        partnership.id.value
    }

    override fun updateAgreementSignedUrl(
        eventSlug: String,
        partnershipId: UUID,
        agreementSignedUrl: String,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.agreementSignedUrl = agreementSignedUrl
        partnership.id.value
    }
}

private class AgreementScope(val agreement: PartnershipAgreement, val pricing: PartnershipPricing)

@Suppress("ThrowsCount")
internal fun OrganisationEntity.toAgreementOrganisation(formatter: DateTimeFormat<LocalDate>): Organisation {
    // Collect all missing required fields
    val missingFields = mutableListOf<String>()

    if (this.headOffice == null) missingFields.add("headOffice")
    if (this.iban == null) missingFields.add("iban")
    if (this.bic == null) missingFields.add("bic")
    if (this.creationLocation == null) missingFields.add("creationLocation")
    if (this.createdAt == null) missingFields.add("createdAt")
    if (this.publishedAt == null) missingFields.add("publishedAt")
    if (this.representativeUser == null) missingFields.add("representativeUser")
    if (this.representativeRole == null) missingFields.add("representativeRole")

    // Throw single exception with all missing fields if any
    if (missingFields.isNotEmpty()) {
        throw ForbiddenException("Fields ${missingFields.joinToString(", ")} are required to perform this operation.")
    }

    return Organisation(
        name = this.name,
        headOffice = this.headOffice!!,
        iban = this.iban!!,
        bic = this.bic!!,
        creationLocation = this.creationLocation!!,
        createdAt = this.createdAt!!.date.format(formatter),
        publishedAt = this.publishedAt!!.date.format(formatter),
        representative = ContactInfo(
            name = this.representativeUser!!.name ?: throw NotFoundException("Representative not found"),
            role = this.representativeRole!!,
        ),
    )
}

internal fun EventEntity.toAgreementEvent(formatter: DateTimeFormat<LocalDate>): Event {
    return Event(
        name = this.name,
        paymentDeadline = this.endTime
            .toInstant(TimeZone.UTC)
            .minus(30.days)
            .toLocalDateTime(TimeZone.UTC)
            .date.format(formatter),
        endDate = LocalDate(this.endTime.year, this.endTime.monthNumber, this.endTime.dayOfMonth)
            .atTime(LocalTime(0, 0, 0))
            .toInstant(TimeZone.UTC)
            .plus(30.days)
            .toLocalDateTime(TimeZone.UTC)
            .date.format(formatter),
    )
}

internal fun CompanyEntity.toAgreementCompany(): Company {
    val missingFields = mutableListOf<String>()

    if (this.siret == null) missingFields.add("siret")
    if (this.address == null) missingFields.add("address")
    if (this.zipCode == null) missingFields.add("zipCode")
    if (this.city == null) missingFields.add("city")
    if (this.country == null) missingFields.add("country")

    // Throw single exception with all missing fields if any
    if (missingFields.isNotEmpty()) {
        throw ForbiddenException("Fields ${missingFields.joinToString(", ")} are required to perform this operation.")
    }
    return Company(
        name = this.name,
        siret = this.siret!!,
        headOffice = "${this.address}, ${this.zipCode} ${this.city}, ${this.country}",
    )
}

@Suppress("ThrowsCount")
internal fun PartnershipEntity.toAgreementPartnership(
    pack: SponsoringPackEntity,
): PartnershipInfo = PartnershipInfo(
    hasBooth = pack.hasBoothFromOptions(),
    contact = ContactInfo(name = this.contactName, role = this.contactRole),
)
