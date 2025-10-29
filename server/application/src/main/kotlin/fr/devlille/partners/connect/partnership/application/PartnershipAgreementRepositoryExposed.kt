package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.pdf.renderMarkdownToPdf
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import fr.devlille.partners.connect.internal.infrastructure.templating.templating
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipAgreementRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.listByPartnershipAndPack
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
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
    override fun generateAgreement(eventSlug: String, partnershipId: UUID): ByteArray = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity.findById(partnershipId) ?: throw NotFoundException("Partnership not found")

        // Eager load relationships to avoid lazy loading outside transaction
        val organisation = event.organisation
        val representativeUser = organisation.representativeUser
        val company = partnership.company
        val pack = partnership.validatedPack()
            ?: throw NotFoundException("Validated pack not found for partnership")
        val options = PartnershipOptionEntity.listByPartnershipAndPack(partnership.id.value, pack.id.value)

        // Preload option translations to avoid lazy loading
        val optionData = options.map { partnershipOption ->
            val sponsoringOption = partnershipOption.option
            val translations = sponsoringOption.translations.toList() // Force load translations
            Pair(partnershipOption, translations)
        }

        val template = readResourceFile("/agreement/${partnership.language}.md")
        val formatter = LocalDate.Format { byUnicodePattern("yyyy/MM/dd") }
        val agreement = Agreement(
            organisation = organisation.toAgreementOrganisation(formatter),
            event = event.toAgreementEvent(formatter),
            company = company.toAgreementCompany(),
            partnership = partnership.toAgreementPartnership(pack, optionData),
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.format(formatter),
            location = "Lille, France",
        )
        val markdown = templating(template, agreement)
        renderMarkdownToPdf(markdown)
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
        representative = Contact(
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

internal fun CompanyEntity.toAgreementCompany(): Company = Company(
    name = this.name,
    siret = this.siret,
    headOffice = "${this.address}, ${this.zipCode} ${this.city}, ${this.country}",
)

@Suppress("ThrowsCount")
internal fun PartnershipEntity.toAgreementPartnership(
    pack: SponsoringPackEntity,
    optionData: List<Pair<PartnershipOptionEntity, List<OptionTranslationEntity>>>,
): Partnership {
    // Use preloaded option translations to avoid lazy loading outside transaction
    val optionTranslations = optionData.map { (partnershipOption, translations) ->
        val translation = translations.firstOrNull { it.language == this.language }
            ?: throw NotFoundException("Translation not found for option ${partnershipOption.id} in language $language")
        Option(name = translation.name)
    }
    val amount = pack.basePrice + optionData.filter { it.first.option.price != null }.sumOf { it.first.option.price!! }
    return Partnership(
        amount = "$amount",
        options = optionTranslations,
        hasBooth = pack.boothSize != null,
        contact = Contact(name = this.contactName, role = this.contactRole),
    )
}

class Agreement(
    val organisation: Organisation,
    val event: Event,
    val company: Company,
    val partnership: Partnership,
    val location: String,
    val createdAt: String,
)

class Event(
    val name: String,
    val paymentDeadline: String,
    val endDate: String,
)

data class Organisation(
    val name: String,
    val headOffice: String,
    val iban: String,
    val bic: String,
    val creationLocation: String,
    val createdAt: String,
    val publishedAt: String,
    val representative: Contact,
)

class Contact(
    val name: String,
    val role: String,
)

class Company(
    val name: String,
    val siret: String,
    val headOffice: String,
)

class Partnership(
    val amount: String,
    val options: List<Option>,
    val hasBooth: Boolean,
    val contact: Contact,
)

class Option(
    val name: String,
)
