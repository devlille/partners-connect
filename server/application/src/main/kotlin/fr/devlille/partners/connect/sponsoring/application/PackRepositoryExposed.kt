package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.sponsoring.application.mappers.toDomain
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listPacksByEvent
import fr.devlille.partners.connect.sponsoring.infrastructure.db.singlePackById
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PackRepositoryExposed(
    private val packEntity: UUIDEntityClass<SponsoringPackEntity> = SponsoringPackEntity,
) : PackRepository {
    override fun findPacksByEvent(eventId: UUID, language: String): List<SponsoringPack> = transaction {
        val packs = packEntity.listPacksByEvent(eventId)
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

    override fun getById(eventId: UUID, packId: UUID, language: String): SponsoringPack = transaction {
        val pack = packEntity.singlePackById(eventId, packId)
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

    override fun createPack(eventId: UUID, input: CreateSponsoringPack): UUID = transaction {
        SponsoringPackEntity.new {
            this.eventId = eventId
            this.name = input.name
            this.basePrice = input.price
            this.withBooth = input.withBooth
            this.nbTickets = input.nbTickets
            this.maxQuantity = input.maxQuantity
        }.id.value
    }

    override fun deletePack(eventId: UUID, packId: UUID) = transaction {
        val pack = packEntity.singlePackById(eventId, packId)
        val hasOptions = PackOptionsTable.listOptionsByPack(pack.id.value).any()
        if (hasOptions) {
            throw BadRequestException("Pack has attached options and cannot be deleted")
        }
        pack.delete()
    }
}
