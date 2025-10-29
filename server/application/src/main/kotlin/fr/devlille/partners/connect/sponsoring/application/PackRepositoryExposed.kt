package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.sponsoring.application.mappers.toDomain
import fr.devlille.partners.connect.sponsoring.application.mappers.toDomainWithAllTranslations
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPackWithTranslations
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listPacksByEvent
import fr.devlille.partners.connect.sponsoring.infrastructure.db.singlePackById
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PackRepositoryExposed : PackRepository {
    override fun findPacksByEvent(eventSlug: String, language: String): List<SponsoringPack> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val packs = SponsoringPackEntity.listPacksByEvent(event.id.value)
        packs.map { pack ->
            pack.toDomain(
                language = language,
                requiredOptionIds = PackOptionsTable.listOptionsByPack(pack.id.value)
                    .filter { it[PackOptionsTable.required] }
                    .map { it[PackOptionsTable.option].value },
                optionalOptions = PackOptionsTable.listOptionsByPack(pack.id.value)
                    .filterNot { it[PackOptionsTable.required] }
                    .map { it[PackOptionsTable.option].value },
            )
        }
    }

    override fun getById(eventSlug: String, packId: UUID, language: String): SponsoringPack = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        pack.toDomain(
            language = language,
            requiredOptionIds = PackOptionsTable.listOptionsByPack(packId)
                .filter { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option].value },
            optionalOptions = PackOptionsTable.listOptionsByPack(packId)
                .filterNot { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option].value },
        )
    }

    override fun createPack(eventSlug: String, input: CreateSponsoringPack): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        SponsoringPackEntity.new {
            this.event = event
            this.name = input.name
            this.basePrice = input.price
            this.boothSize = input.boothSize
            this.nbTickets = input.nbTickets
            this.maxQuantity = input.maxQuantity
        }.id.value
    }

    override fun updatePack(eventSlug: String, packId: UUID, input: CreateSponsoringPack): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        pack.name = input.name
        pack.basePrice = input.price
        pack.boothSize = input.boothSize
        pack.nbTickets = input.nbTickets
        pack.maxQuantity = input.maxQuantity
        pack.id.value
    }

    override fun deletePack(eventSlug: String, packId: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        val hasOptions = PackOptionsTable.listOptionsByPack(pack.id.value).any()
        if (hasOptions) {
            throw ForbiddenException("Pack has attached options and cannot be deleted")
        }
        pack.delete()
    }

    override fun findPacksByEventWithAllTranslations(
        eventSlug: String,
    ): List<SponsoringPackWithTranslations> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val packs = SponsoringPackEntity.listPacksByEvent(event.id.value)
        packs.map { pack ->
            pack.toDomainWithAllTranslations(
                requiredOptionIds = PackOptionsTable.listOptionsByPack(pack.id.value)
                    .filter { it[PackOptionsTable.required] }
                    .map { it[PackOptionsTable.option].value },
                optionalOptions = PackOptionsTable.listOptionsByPack(pack.id.value)
                    .filterNot { it[PackOptionsTable.required] }
                    .map { it[PackOptionsTable.option].value },
            )
        }
    }

    override fun getByIdWithAllTranslations(
        eventSlug: String,
        packId: UUID,
    ): SponsoringPackWithTranslations = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        pack.toDomainWithAllTranslations(
            requiredOptionIds = PackOptionsTable.listOptionsByPack(packId)
                .filter { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option].value },
            optionalOptions = PackOptionsTable.listOptionsByPack(packId)
                .filterNot { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option].value },
        )
    }
}
