package fr.devlille.partners.connect.sponsoring.infrastructure.db

import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class SponsoringPackEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SponsoringPackEntity>(SponsoringPacksTable)

    var eventId by SponsoringPacksTable.eventId
    var name by SponsoringPacksTable.name
    var basePrice by SponsoringPacksTable.basePrice
    var withBooth by SponsoringPacksTable.withBooth
    var maxQuantity by SponsoringPacksTable.maxQuantity
    val options by SponsoringOptionEntity via PackOptionsTable
}

fun UUIDEntityClass<SponsoringPackEntity>.listPacksByEvent(eventId: UUID): List<SponsoringPackEntity> = this
    .find { SponsoringPacksTable.eventId eq eventId }
    .toList()

fun UUIDEntityClass<SponsoringPackEntity>.singlePackById(eventId: UUID, packId: UUID): SponsoringPackEntity = this
    .find { (SponsoringPacksTable.id eq packId) and (SponsoringPacksTable.eventId eq eventId) }
    .singleOrNull()
    ?: throw NotFoundException("Pack not found")
