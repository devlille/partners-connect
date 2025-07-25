package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

object PackOptionsTable : Table("pack_options") {
    val pack = reference("pack_id", SponsoringPacksTable)
    val option = reference("option_id", SponsoringOptionsTable)
    val required = bool("required").default(false)
    override val primaryKey = PrimaryKey(pack, option)
}

fun PackOptionsTable.listOptionsByPack(packId: UUID): List<ResultRow> = this
    .selectAll()
    .where { PackOptionsTable.pack eq packId }
    .toList()

fun PackOptionsTable.listOptionalOptionsByPack(packId: UUID): List<ResultRow> = this
    .selectAll()
    .where { (PackOptionsTable.pack eq packId) and (PackOptionsTable.required eq false) }
    .toList()

fun PackOptionsTable.listOptionsAttachedByEventAndOption(eventId: UUID, optionId: UUID): Query = this
    .innerJoin(SponsoringPacksTable)
    .selectAll()
    .where { (SponsoringPacksTable.eventId eq eventId) and (PackOptionsTable.option eq optionId) }
