package fr.devlille.partners.connect.sponsoring.infrastructure.db

import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

fun UUIDEntityClass<SponsoringPackEntity>.getPacksByEvent(eventId: UUID): List<SponsoringPackEntity> = this
    .find { SponsoringPacksTable.eventId eq eventId }
    .toList()

fun UUIDEntityClass<SponsoringPackEntity>.getPackById(eventId: UUID, packId: UUID): SponsoringPackEntity = this
    .find { (SponsoringPacksTable.id eq packId) and (SponsoringPacksTable.eventId eq eventId) }
    .singleOrNull()
    ?: throw NotFoundException("Pack not found")

fun PackOptionsTable.getOptionsByPack(packId: UUID): List<ResultRow> = this
    .selectAll()
    .where { PackOptionsTable.pack eq packId }
    .toList()
