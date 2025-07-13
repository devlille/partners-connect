package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class OptionTranslationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<OptionTranslationEntity>(OptionTranslationsTable)

    var option by SponsoringOptionEntity referencedOn OptionTranslationsTable.option
    var language by OptionTranslationsTable.language
    var name by OptionTranslationsTable.name
    var description by OptionTranslationsTable.description
}
