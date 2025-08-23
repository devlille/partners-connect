package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.sponsoring.application.mappers.toDomain
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
import fr.devlille.partners.connect.sponsoring.infrastructure.db.allByEvent
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsAttachedByEventAndOption
import fr.devlille.partners.connect.sponsoring.infrastructure.db.singlePackById
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class OptionRepositoryExposed(
    private val optionEntity: UUIDEntityClass<SponsoringOptionEntity> = SponsoringOptionEntity,
) : OptionRepository {
    override fun listOptionsByEvent(eventSlug: String, language: String): List<SponsoringOption> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        optionEntity.allByEvent(event.id.value).map { option -> option.toDomain(language) }
    }

    override fun createOption(eventSlug: String, input: CreateSponsoringOption): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val option = optionEntity.new {
            this.event = event
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
        option.id.value
    }

    override fun updateOption(eventSlug: String, optionId: UUID, input: CreateSponsoringOption): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val option = optionEntity.findById(optionId) 
            ?: throw NotFoundException(
                code = ErrorCode.INTEGRATION_NOT_FOUND,
                message = "Option not found",
                meta = mapOf(MetaKeys.ID to optionId.toString()),
            )
        if (option.event.id.value != event.id.value) throw NotFoundException(
            code = ErrorCode.INTEGRATION_NOT_FOUND,
            message = "Option not found",
            meta = mapOf(MetaKeys.ID to optionId.toString()),
        )

        option.price = input.price

        // Replace translations - delete existing and insert new ones
        OptionTranslationsTable.deleteWhere { OptionTranslationsTable.option eq optionId }
        input.translations.forEach { translation ->
            OptionTranslationEntity.new {
                this.option = option
                this.language = translation.language
                this.name = translation.name
                this.description = translation.description
            }
        }

        option.id.value
    }

    override fun deleteOption(eventSlug: String, optionId: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val isUsed = PackOptionsTable
            .listOptionsAttachedByEventAndOption(event.id.value, optionId)
            .empty().not()
        if (isUsed) throw BadRequestException("Option is used in a pack and cannot be deleted")

        // Delete translations first (FK constraint)
        OptionTranslationsTable.deleteWhere { OptionTranslationsTable.option eq optionId }

        // Then delete option
        val deleted = SponsoringOptionsTable.deleteWhere {
            (SponsoringOptionsTable.id eq optionId) and (SponsoringOptionsTable.eventId eq event.id.value)
        }

        if (deleted == 0) throw NotFoundException(
            code = ErrorCode.INTEGRATION_NOT_FOUND,
            message = "Option not found",
            meta = mapOf(),
        )
    }

    override fun attachOptionsToPack(eventSlug: String, packId: UUID, options: AttachOptionsToPack) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val intersect = options.required.intersect(options.optional)
        if (intersect.isNotEmpty()) {
            throw BadRequestException("options ${intersect.joinToString(",")} cannot be both required and optional")
        }

        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)

        val requiredOptions = SponsoringOptionEntity
            .find {
                (SponsoringOptionsTable.eventId eq event.id.value) and
                    (SponsoringOptionsTable.id inList options.required.map(UUID::fromString))
            }

        val optionalOptions = SponsoringOptionEntity
            .find {
                (SponsoringOptionsTable.eventId eq event.id.value) and
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
            .where { (PackOptionsTable.pack eq packId) and (PackOptionsTable.option inList allOptionIds) }
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

    override fun detachOptionFromPack(eventSlug: String, packId: UUID, optionId: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException(
                code = ErrorCode.EVENT_NOT_FOUND,
                message = "Event with slug $eventSlug not found",
                meta = mapOf(MetaKeys.EVENT to eventSlug),
            )
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        val deleted = PackOptionsTable.deleteWhere {
            (PackOptionsTable.pack eq pack.id) and (PackOptionsTable.option eq optionId)
        }
        if (deleted == 0) {
            throw NotFoundException(
                code = ErrorCode.INTEGRATION_NOT_FOUND,
                message = "Option not attached to pack",
                meta = mapOf(),
            )
        }
    }
}
