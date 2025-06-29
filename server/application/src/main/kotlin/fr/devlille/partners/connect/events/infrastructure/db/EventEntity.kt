package fr.devlille.partners.connect.events.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EventEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventEntity>(EventsTable)

    var name by EventsTable.name
    var startTime by EventsTable.startTime
    var endTime by EventsTable.endTime
    var submissionStartTime by EventsTable.submissionStartTime
    var submissionEndTime by EventsTable.submissionEndTime
    var address by EventsTable.address
    var contactPhone by EventsTable.contactPhone
    var contactEmail by EventsTable.contactEmail
    var legalName by EventsTable.legalName
    var siret by EventsTable.siret
    var siren by EventsTable.siren
    var tva by EventsTable.tva
    var dAndB by EventsTable.dAndB
    var nace by EventsTable.nace
    var naf by EventsTable.naf
    var duns by EventsTable.duns
    var iban by EventsTable.iban
    var bic by EventsTable.bic
    var ribUrl by EventsTable.ribUrl
}
