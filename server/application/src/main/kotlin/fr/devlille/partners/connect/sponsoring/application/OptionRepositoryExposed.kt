package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOption
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class OptionRepositoryExposed : OptionRepository {
    override fun listOptionsByEvent(eventId: String, language: String): List<SponsoringOption> = transaction {
        SponsoringOptionEntity.find { SponsoringOptionsTable.eventId eq UUID.fromString(eventId) }
            .map { option -> option.toSponsoringOption(language) }
    }

    override fun createOption(eventId: String, input: CreateSponsoringOption): String = transaction {
        val option = SponsoringOptionEntity.new {
            this.eventId = UUID.fromString(eventId)
            this.price = input.price
        }
        input.translations.forEach {
            OptionTranslationEntity.new {
                this.option = option
                this.language = it.language
                this.name = it.name
                this.description = it.description
            }
        }
        option.id.value.toString()
    }

    override fun deleteOption(eventId: String, optionId: String) = transaction {
        val optionUUID = UUID.fromString(optionId)
        val eventUUID = UUID.fromString(eventId)

        val isUsed = PackOptionsTable.innerJoin(SponsoringPacksTable)
            .selectAll()
            .where {
                (PackOptionsTable.option eq optionUUID) and
                    (SponsoringPacksTable.eventId eq eventUUID)
            }.count() > 0

        if (isUsed) throw BadRequestException("Option is used in a pack and cannot be deleted")

        // Delete translations first (FK constraint)
        OptionTranslationsTable.deleteWhere { OptionTranslationsTable.option eq optionUUID }

        // Then delete option
        val deleted = SponsoringOptionsTable.deleteWhere {
            (SponsoringOptionsTable.id eq optionUUID) and (SponsoringOptionsTable.eventId eq eventUUID)
        }

        if (deleted == 0) throw NotFoundException("Option not found")
    }

    override fun attachOptionsToPack(eventId: String, packId: String, options: AttachOptionsToPack) = transaction {
        val packUUID = UUID.fromString(packId)
        val eventUUID = UUID.fromString(eventId)

        val intersect = options.required.intersect(options.optional)
        if (intersect.isNotEmpty()) {
            throw BadRequestException("options ${intersect.joinToString(",")} cannot be both required and optional")
        }

        val pack = SponsoringPackEntity
            .find { (SponsoringPacksTable.id eq packUUID) and (SponsoringPacksTable.eventId eq eventUUID) }
            .firstOrNull()
            ?: throw NotFoundException("Pack not found")

        val requiredOptions = SponsoringOptionEntity
            .find {
                (SponsoringOptionsTable.eventId eq eventUUID) and
                    (SponsoringOptionsTable.id inList options.required.map(UUID::fromString))
            }

        val optionalOptions = SponsoringOptionEntity
            .find {
                (SponsoringOptionsTable.eventId eq eventUUID) and
                    (SponsoringOptionsTable.id inList options.optional.map(UUID::fromString))
            }.toList()

        val existingOptions = (requiredOptions + optionalOptions)
            .map { option -> option.id.value.toString() }
        val allOptionIds = (options.required + options.optional)
            .map(UUID::fromString)
            .distinct()

        if (existingOptions.size != allOptionIds.size) {
            throw BadRequestException("Some options do not belong to the event")
        }

        val alreadyAttached = PackOptionsTable
            .selectAll()
            .where { (PackOptionsTable.pack eq packUUID) and (PackOptionsTable.option inList allOptionIds) }
            .map { it[PackOptionsTable.option].value }

        if (alreadyAttached.isNotEmpty()) {
            throw BadRequestException("Option already attached to pack: ${alreadyAttached.joinToString()}")
        }

        requiredOptions.forEach { option ->
            PackOptionsTable.insert {
                it[this.pack] = pack.id
                it[this.option] = option.id
                it[this.required] = true
            }
        }
        optionalOptions.forEach { option ->
            PackOptionsTable.insert {
                it[this.pack] = pack.id
                it[this.option] = option.id
                it[this.required] = false
            }
        }
    }

    override fun detachOptionFromPack(eventId: String, packId: String, optionId: String) = transaction {
        val packUUID = UUID.fromString(packId)
        val optionUUID = UUID.fromString(optionId)
        val eventUUID = UUID.fromString(eventId)

        val pack = SponsoringPackEntity.find {
            (SponsoringPacksTable.id eq packUUID) and (SponsoringPacksTable.eventId eq eventUUID)
        }.firstOrNull() ?: throw NotFoundException("Pack not found")

        val deleted = PackOptionsTable.deleteWhere {
            (PackOptionsTable.pack eq pack.id) and (PackOptionsTable.option eq optionUUID)
        }

        if (deleted == 0) {
            throw NotFoundException("Option not attached to pack")
        }
    }
}
