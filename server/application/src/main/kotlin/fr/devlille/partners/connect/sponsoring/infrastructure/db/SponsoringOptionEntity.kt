package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class SponsoringOptionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SponsoringOptionEntity>(SponsoringOptionsTable)

    var eventId by SponsoringOptionsTable.eventId
    var price by SponsoringOptionsTable.price
    val translations by OptionTranslationEntity referrersOn OptionTranslationsTable.option
}

fun UUIDEntityClass<SponsoringOptionEntity>.allByEvent(eventId: UUID): SizedIterable<SponsoringOptionEntity> = this
    .find { SponsoringOptionsTable.eventId eq eventId }
