package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PackRepositoryExposed : PackRepository {
    override fun findPacksByEvent(eventId: String, language: String): List<SponsoringPack> = transaction {
        val eventUUID = UUID.fromString(eventId)
        val packs: List<SponsoringPackEntity> = SponsoringPackEntity
            .find { SponsoringPacksTable.eventId eq eventUUID }
            .toList()
        packs.map { pack ->
            val packOptions = PackOptionsTable.selectAll()
                .where { PackOptionsTable.pack eq pack.id }
                .toList()
            val requiredOptionIds = packOptions
                .filter { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option] }
            val optionalOptions = packOptions
                .filterNot { it[PackOptionsTable.required] }
                .map { it[PackOptionsTable.option] }
            SponsoringPack(
                id = pack.id.value.toString(),
                name = pack.name,
                basePrice = pack.basePrice,
                maxQuantity = pack.maxQuantity,
                requiredOptions = pack.options
                    .filter { requiredOptionIds.contains(it.id) }
                    .map { option -> option.toSponsoringOption(language) },
                optionalOptions = pack.options
                    .filter { optionalOptions.contains(it.id) }
                    .map { option -> option.toSponsoringOption(language) },
            )
        }
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

        val pack = SponsoringPackEntity.find {
            (SponsoringPacksTable.id eq packUUID) and (SponsoringPacksTable.eventId eq eventUUID)
        }.firstOrNull() ?: throw NotFoundException("Pack not found")

        val hasOptions = PackOptionsTable.selectAll().where { PackOptionsTable.pack eq pack.id }.any()
        if (hasOptions) {
            throw BadRequestException("Pack has attached options and cannot be deleted")
        }

        pack.delete()
    }
}
