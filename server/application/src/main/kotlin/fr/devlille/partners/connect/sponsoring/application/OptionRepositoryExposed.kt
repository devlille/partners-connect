package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.sponsoring.application.mappers.toDomainWithAllTranslations
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateText
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedNumber
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedQuantitative
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedSelectable
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOptionWithTranslations
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SelectableValueEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.allByEvent
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsAttachedByEventAndOption
import fr.devlille.partners.connect.sponsoring.infrastructure.db.singlePackById
import io.ktor.server.plugins.NotFoundException
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
    override fun createOption(eventSlug: String, input: CreateSponsoringOption): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val option = optionEntity.new {
            this.event = event
            this.price = input.price
            // Set polymorphic fields based on the sealed class variant
            when (input) {
                is CreateText -> {
                    this.optionType = OptionType.TEXT
                    this.quantitativeDescriptor = null
                    this.numberDescriptor = null
                    this.selectableDescriptor = null
                    this.fixedQuantity = null
                }
                is CreateTypedQuantitative -> {
                    this.optionType = OptionType.TYPED_QUANTITATIVE
                    this.quantitativeDescriptor = input.typeDescriptor
                    this.numberDescriptor = null
                    this.selectableDescriptor = null
                    this.fixedQuantity = null
                }
                is CreateTypedNumber -> {
                    this.optionType = OptionType.TYPED_NUMBER
                    this.quantitativeDescriptor = null
                    this.numberDescriptor = input.typeDescriptor
                    this.selectableDescriptor = null
                    this.fixedQuantity = input.fixedQuantity
                }
                is CreateTypedSelectable -> {
                    this.optionType = OptionType.TYPED_SELECTABLE
                    this.quantitativeDescriptor = null
                    this.numberDescriptor = null
                    this.selectableDescriptor = input.typeDescriptor
                    this.fixedQuantity = null
                }
            }
        }
        input.translations.forEach {
            OptionTranslationEntity.new {
                this.option = option
                this.language = it.language
                this.name = it.name
                this.description = it.description
            }
        }
        // Create selectable values if provided (only for selectable type)
        if (input is CreateTypedSelectable) {
            input.selectableValues.forEach { selectableValue ->
                SelectableValueEntity.createForOption(option.id.value, selectableValue.value, selectableValue.price)
            }
        }
        option.id.value
    }

    override fun updateOption(eventSlug: String, optionId: UUID, input: CreateSponsoringOption): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val option = optionEntity.findById(optionId) ?: throw NotFoundException("Option not found")
        if (option.event.id.value != event.id.value) throw NotFoundException("Option not found")

        option.price = input.price
        // Update polymorphic fields based on the sealed class variant
        when (input) {
            is CreateText -> {
                option.optionType = OptionType.TEXT
                option.quantitativeDescriptor = null
                option.numberDescriptor = null
                option.selectableDescriptor = null
                option.fixedQuantity = null
            }
            is CreateTypedQuantitative -> {
                option.optionType = OptionType.TYPED_QUANTITATIVE
                option.quantitativeDescriptor = input.typeDescriptor
                option.numberDescriptor = null
                option.selectableDescriptor = null
                option.fixedQuantity = null
            }
            is CreateTypedNumber -> {
                option.optionType = OptionType.TYPED_NUMBER
                option.quantitativeDescriptor = null
                option.numberDescriptor = input.typeDescriptor
                option.selectableDescriptor = null
                option.fixedQuantity = input.fixedQuantity
            }
            is CreateTypedSelectable -> {
                option.optionType = OptionType.TYPED_SELECTABLE
                option.quantitativeDescriptor = null
                option.numberDescriptor = null
                option.selectableDescriptor = input.typeDescriptor
                option.fixedQuantity = null
            }
        }

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

        // Update selectable values - delete existing and create new ones (always clean up)
        SelectableValueEntity.deleteAllByOption(optionId)
        if (input is CreateTypedSelectable) {
            input.selectableValues.forEach { selectableValue ->
                SelectableValueEntity.createForOption(optionId, selectableValue.value, selectableValue.price)
            }
        }

        option.id.value
    }

    override fun deleteOption(eventSlug: String, optionId: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Check if option is used in packs
        val isUsedInPacks = PackOptionsTable
            .listOptionsAttachedByEventAndOption(event.id.value, optionId)
            .empty().not()
        if (isUsedInPacks) throw ForbiddenException("Option is used in a pack and cannot be deleted")

        // Check if option is used in partnerships
        val isUsedInPartnerships = PartnershipOptionsTable
            .selectAll()
            .where { PartnershipOptionsTable.optionId eq optionId }
            .empty().not()
        if (isUsedInPartnerships) throw ForbiddenException("Option is used in partnerships and cannot be deleted")

        // Delete translations first (FK constraint)
        OptionTranslationsTable.deleteWhere { OptionTranslationsTable.option eq optionId }

        // Then delete option
        val deleted = SponsoringOptionsTable.deleteWhere {
            (SponsoringOptionsTable.id eq optionId) and (SponsoringOptionsTable.eventId eq event.id.value)
        }

        if (deleted == 0) throw NotFoundException("Option not found")
    }

    /**
     * Synchronizes pack options by removing options not in the submitted lists,
     * adding new options, and updating requirement status for existing options.
     *
     * This operation is atomic (all changes succeed or all fail) and idempotent
     * (submitting the same configuration multiple times produces the same result).
     *
     * @param eventSlug The event identifier
     * @param packId The pack identifier
     * @param options Complete configuration of required and optional options
     * @throws NotFoundException if event or pack not found
     * @throws ConflictException if same option in both required and optional lists
     * @throws ForbiddenException if any option doesn't belong to the event
     */
    override fun attachOptionsToPack(eventSlug: String, packId: UUID, options: AttachOptionsToPack) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val intersect = options.required.intersect(options.optional)
        if (intersect.isNotEmpty()) {
            throw ConflictException("options ${intersect.joinToString(",")} cannot be both required and optional")
        }

        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)

        val requiredOptions = SponsoringOptionEntity
            .find {
                (SponsoringOptionsTable.eventId eq event.id.value) and
                    (SponsoringOptionsTable.id inList options.required.map(UUID::fromString))
            }.toList()

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
            throw ForbiddenException("Some options do not belong to the event")
        }

        // 1. Delete all existing pack options (synchronization - will re-insert desired state)
        PackOptionsTable.deleteWhere { PackOptionsTable.pack eq packId }

        // 2. Insert required options
        requiredOptions.forEach { option ->
            PackOptionsTable.insert {
                it[this.pack] = pack.id
                it[this.option] = option.id
                it[this.required] = true
            }
        }

        // 3. Insert optional options
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
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        val deleted = PackOptionsTable.deleteWhere {
            (PackOptionsTable.pack eq pack.id) and (PackOptionsTable.option eq optionId)
        }
        if (deleted == 0) {
            throw NotFoundException("Option not attached to pack")
        }
    }

    override fun listOptionsByEventWithAllTranslations(
        eventSlug: String,
    ): List<SponsoringOptionWithTranslations> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Use the existing extension function which is working correctly
        optionEntity
            .allByEvent(event.id.value)
            .map { option -> option.toDomainWithAllTranslations() }
    }

    override fun getOptionByIdWithAllTranslations(
        eventSlug: String,
        optionId: UUID,
    ): SponsoringOptionWithTranslations = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val option = optionEntity.findById(optionId) ?: throw NotFoundException("Option not found")

        if (option.event.id.value != event.id.value) {
            throw NotFoundException("Option not found")
        }

        option.toDomainWithAllTranslations()
    }
}
