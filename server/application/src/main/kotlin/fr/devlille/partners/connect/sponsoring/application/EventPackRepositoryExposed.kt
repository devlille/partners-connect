package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.sponsoring.application.mappers.toDomain
import fr.devlille.partners.connect.sponsoring.domain.EventPackRepository
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsByPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listPacksByEvent
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Exposed-based implementation of EventPackRepository.
 *
 * Reuses existing database schema and query infrastructure from PackRepositoryExposed
 * but provides public access without authentication or authorization checks.
 */
class EventPackRepositoryExposed : EventPackRepository {
    override fun findPublicPacksByEvent(eventSlug: String, language: String): List<SponsoringPack> = transaction {
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
}
