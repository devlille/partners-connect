package fr.devlille.partners.connect.agenda.infrastructure.db

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class SpeakerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpeakerEntity>(SpeakersTable)

    var externalId by SpeakersTable.externalId
    var name by SpeakersTable.name
    var biography by SpeakersTable.biography
    var photoUrl by SpeakersTable.photoUrl
    var jobTitle by SpeakersTable.jobTitle
    var pronouns by SpeakersTable.pronouns
    var createdAt by SpeakersTable.createdAt

    var event by EventEntity referencedOn SpeakersTable.eventId
    var company by CompanyEntity optionalReferencedOn SpeakersTable.companyId
}
