package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.EventEntity
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummaryEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

class EventRepositoryDefault(
    private val table: EventsTable
) : EventRepository {
    override fun getAllEvents(): List<EventSummaryEntity> = transaction {
        table.selectAll().map {
            EventSummaryEntity(
                id = it[table.id].value,
                name = it[table.name],
                startTime = it[table.startTime],
                endTime = it[table.endTime],
                submissionStartTime = it[table.submissionStartTime],
                submissionEndTime = it[table.submissionEndTime]
            )
        }
    }

    override fun createEvent(event: EventEntity) {
        transaction {
            table.insert {
                it[id] = event.id
                it[name] = event.name
                it[startTime] = event.startTime
                it[endTime] = event.endTime
                it[submissionStartTime] = event.submissionStartTime
                it[submissionEndTime] = event.submissionEndTime
                it[address] = event.address
                it[contactPhone] = event.contactPhone
                it[contactEmail] = event.contactEmail
                it[legalName] = event.legalName
                it[siret] = event.siret
                it[siren] = event.siren
                it[tva] = event.tva
                it[dAndB] = event.dAndB
                it[nace] = event.nace
                it[naf] = event.naf
                it[duns] = event.duns
                it[iban] = event.iban
                it[bic] = event.bic
                it[ribUrl] = event.ribUrl
            }
        }
    }

    override fun updateEvent(event: EventEntity) {
        transaction {
            table.update({ table.id eq event.id }) {
                it[name] = event.name
                it[startTime] = event.startTime
                it[endTime] = event.endTime
                it[submissionStartTime] = event.submissionStartTime
                it[submissionEndTime] = event.submissionEndTime
                it[address] = event.address
                it[contactPhone] = event.contactPhone
                it[contactEmail] = event.contactEmail
                it[legalName] = event.legalName
                it[siret] = event.siret
                it[siren] = event.siren
                it[tva] = event.tva
                it[dAndB] = event.dAndB
                it[nace] = event.nace
                it[naf] = event.naf
                it[duns] = event.duns
                it[iban] = event.iban
                it[bic] = event.bic
                it[ribUrl] = event.ribUrl
            }
        }
    }
}
