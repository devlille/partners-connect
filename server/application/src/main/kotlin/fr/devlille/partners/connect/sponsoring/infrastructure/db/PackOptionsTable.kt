package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.Table

object PackOptionsTable : Table("pack_options") {
    val pack = reference("pack_id", SponsoringPacksTable)
    val option = reference("option_id", SponsoringOptionsTable)
    val required = bool("required").default(false)
    override val primaryKey = PrimaryKey(pack, option)
}
