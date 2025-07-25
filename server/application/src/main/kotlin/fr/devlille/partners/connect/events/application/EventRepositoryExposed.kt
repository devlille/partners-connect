package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummary
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
                id = it.id.value,
                name = it.name,
                startTime = it.startTime,
                endTime = it.endTime,
                submissionStartTime = it.submissionStartTime,
                submissionEndTime = it.submissionEndTime,
            )
        }
    }

    override fun getById(eventId: String): Event = transaction {
        val event = entity.findById(UUID.fromString(eventId))
            ?: throw NotFoundException("Event with id $eventId not found")
        Event(
            id = event.id.value,
            name = event.name,
            startTime = event.startTime,
            endTime = event.endTime,
            submissionStartTime = event.submissionStartTime,
            submissionEndTime = event.submissionEndTime,
            address = event.address,
            contactPhone = event.contactPhone,
            contactEmail = event.contactEmail,
            legalName = event.legalName,
            siret = event.siret,
            siren = event.siren,
            tva = event.tva,
            dAndB = event.dAndB,
            nace = event.nace,
            naf = event.naf,
            duns = event.duns,
            iban = event.iban,
            bic = event.bic,
            ribUrl = event.ribUrl,
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
            contactPhone = event.contactPhone
            contactEmail = event.contactEmail
            legalName = event.legalName
            siret = event.siret
            siren = event.siren
            tva = event.tva
            dAndB = event.dAndB
            nace = event.nace
            naf = event.naf
            duns = event.duns
            iban = event.iban
            bic = event.bic
            ribUrl = event.ribUrl
        }.id.value
    }

    override fun updateEvent(event: Event): UUID = transaction {
        val entity = entity.findById(event.id) ?: throw IllegalArgumentException("Event not found")

        entity.name = event.name
        entity.startTime = event.startTime
        entity.endTime = event.endTime
        entity.submissionStartTime = event.submissionStartTime
        entity.submissionEndTime = event.submissionEndTime
        entity.address = event.address
        entity.contactPhone = event.contactPhone
        entity.contactEmail = event.contactEmail
        entity.legalName = event.legalName
        entity.siret = event.siret
        entity.siren = event.siren
        entity.tva = event.tva
        entity.dAndB = event.dAndB
        entity.nace = event.nace
        entity.naf = event.naf
        entity.duns = event.duns
        entity.iban = event.iban
        entity.bic = event.bic
        entity.ribUrl = event.ribUrl

        entity.id.value
    }
}
