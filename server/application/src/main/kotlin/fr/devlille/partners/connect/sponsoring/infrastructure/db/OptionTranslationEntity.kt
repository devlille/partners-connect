package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.UUID

class OptionTranslationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<OptionTranslationEntity>(OptionTranslationsTable)

    var option by SponsoringOptionEntity referencedOn OptionTranslationsTable.option
    var language by OptionTranslationsTable.language
    var name by OptionTranslationsTable.name
    var description by OptionTranslationsTable.description
}

fun UUIDEntityClass<OptionTranslationEntity>.listTranslationsByOptionAndLanguage(
    optionId: UUID,
    language: String,
): List<OptionTranslationEntity> = this
    .find { (OptionTranslationsTable.option eq optionId) and (OptionTranslationsTable.language eq language) }
    .toList()
