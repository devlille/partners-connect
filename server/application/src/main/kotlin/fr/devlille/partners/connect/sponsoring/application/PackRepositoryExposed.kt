package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.sponsoring.application.mappers.toDomain
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.getOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.getPackById
import fr.devlille.partners.connect.sponsoring.infrastructure.db.getPacksByEvent
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PackRepositoryExposed(
    private val packEntity: UUIDEntityClass<SponsoringPackEntity> = SponsoringPackEntity,
) : PackRepository {
    override fun findPacksByEvent(eventId: String, language: String): List<SponsoringPack> = transaction {
        val eventUUID = UUID.fromString(eventId)
        val packs = packEntity.getPacksByEvent(eventUUID)
        packs.map { pack ->
            pack.toDomain(
                language = language,
                requiredOptionIds = PackOptionsTable.getOptionsByPack(pack.id.value)
                    .filter { it[PackOptionsTable.required] }
                    .map { it[PackOptionsTable.option].value },
                optionalOptions = PackOptionsTable.getOptionsByPack(pack.id.value)
                    .filterNot { it[PackOptionsTable.required] }
                    .map { it[PackOptionsTable.option].value },
            )
        }
    }

    override fun getById(eventId: String, packId: String, language: String): SponsoringPack = transaction {
        val eventUUID = UUID.fromString(eventId)
        val packUUID = UUID.fromString(packId)
        val pack = packEntity.getPackById(eventUUID, packUUID)
        pack.toDomain(
            language = language,
            requiredOptionIds = PackOptionsTable.getOptionsByPack(packUUID)
                .filter { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option].value },
            optionalOptions = PackOptionsTable.getOptionsByPack(packUUID)
                .filterNot { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option].value },
        )
    }

    override fun createPack(eventId: String, input: CreateSponsoringPack): String = transaction {
        val eventUUID = UUID.fromString(eventId)
        SponsoringPackEntity.new {
            this.eventId = eventUUID
            this.name = input.name
            this.basePrice = input.price
            this.maxQuantity = input.maxQuantity
        }.id.value.toString()
    }

    override fun deletePack(eventId: String, packId: String) = transaction {
        val packUUID = UUID.fromString(packId)
        val eventUUID = UUID.fromString(eventId)
        val pack = packEntity.getPackById(eventUUID, packUUID)
        val hasOptions = PackOptionsTable.getOptionsByPack(pack.id.value).any()
        if (hasOptions) {
            throw BadRequestException("Pack has attached options and cannot be deleted")
        }
        pack.delete()
    }
}
