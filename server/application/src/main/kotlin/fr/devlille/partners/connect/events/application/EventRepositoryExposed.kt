package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.Banking
import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.domain.Legal
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EventRepositoryExposed(
    private val entity: UUIDEntityClass<EventEntity>,
) : EventRepository {
    override fun getAllEvents(): List<EventSummary> = transaction {
        entity.all().map {
            EventSummary(
                id = it.id.value.toString(),
                name = it.name,
                startTime = it.startTime,
                endTime = it.endTime,
                submissionStartTime = it.submissionStartTime,
                submissionEndTime = it.submissionEndTime,
            )
        }
    }

    override fun getById(eventId: UUID): Event = transaction {
        val event = entity.findById(eventId)
            ?: throw NotFoundException("Event with id $eventId not found")
        Event(
            name = event.name,
            startTime = event.startTime,
            endTime = event.endTime,
            submissionStartTime = event.submissionStartTime,
            submissionEndTime = event.submissionEndTime,
            address = event.address,
            contact = Contact(phone = event.contactPhone, email = event.contactEmail),
            legal = Legal(
                name = event.legalName,
                siret = event.siret,
                siren = event.siren,
                tva = event.tva,
                dAndB = event.dAndB,
                nace = event.nace,
                naf = event.naf,
                duns = event.duns,
            ),
            banking = Banking(
                iban = event.iban,
                bic = event.bic,
                ribUrl = event.ribUrl,
            ),
        )
    }

    override fun createEvent(event: Event): UUID = transaction {
        entity.new {
            name = event.name
            startTime = event.startTime
            endTime = event.endTime
            submissionStartTime = event.submissionStartTime
            submissionEndTime = event.submissionEndTime
            address = event.address
            contactPhone = event.contact.phone
            contactEmail = event.contact.email
            legalName = event.legal?.name
            siret = event.legal?.siret
            siren = event.legal?.siren
            tva = event.legal?.tva
            dAndB = event.legal?.dAndB
            nace = event.legal?.nace
            naf = event.legal?.naf
            duns = event.legal?.duns
            iban = event.banking?.iban
            bic = event.banking?.bic
            ribUrl = event.banking?.ribUrl
        }.id.value
    }

    override fun updateEvent(id: UUID, event: Event): UUID = transaction {
        val entity = entity.findById(id) ?: throw IllegalArgumentException("Event not found")

        entity.name = event.name
        entity.startTime = event.startTime
        entity.endTime = event.endTime
        entity.submissionStartTime = event.submissionStartTime
        entity.submissionEndTime = event.submissionEndTime
        entity.address = event.address
        entity.contactPhone = event.contact.phone
        entity.contactEmail = event.contact.email
        entity.legalName = event.legal?.name
        entity.siret = event.legal?.siret
        entity.siren = event.legal?.siren
        entity.tva = event.legal?.tva
        entity.dAndB = event.legal?.dAndB
        entity.nace = event.legal?.nace
        entity.naf = event.legal?.naf
        entity.duns = event.legal?.duns
        entity.iban = event.banking?.iban
        entity.bic = event.banking?.bic
        entity.ribUrl = event.banking?.ribUrl

        entity.id.value
    }
}
