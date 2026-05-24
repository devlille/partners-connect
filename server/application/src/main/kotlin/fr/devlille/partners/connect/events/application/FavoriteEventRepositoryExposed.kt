package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.application.mappers.toEventSummary
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.domain.FavoriteEventRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.events.infrastructure.db.listByUserOrderByEventStartTime
import fr.devlille.partners.connect.events.infrastructure.db.singleFavorite
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.hasPermission
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FavoriteEventRepositoryExposed : FavoriteEventRepository {
    override fun listByUserEmail(userEmail: String): List<EventSummary> = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException("User with email $userEmail not found")
        FavoriteEventEntity
            .listByUserOrderByEventStartTime(user.id.value)
            .filter { favorite ->
                OrganisationPermissionEntity.hasPermission(
                    organisationId = favorite.event.organisation.id.value,
                    userId = user.id.value,
                )
            }
            .map { it.event.toEventSummary() }
    }

    override fun addFavorite(userEmail: String, eventSlug: String): Boolean = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException("User with email $userEmail not found")
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event $eventSlug not found")
        if (!OrganisationPermissionEntity.hasPermission(event.organisation.id.value, user.id.value)) {
            throw ForbiddenException("You are not allowed to favorite this event")
        }
        if (FavoriteEventEntity.singleFavorite(user.id.value, event.id.value) != null) {
            return@transaction false
        }
        FavoriteEventEntity.new {
            this.user = user
            this.event = event
        }
        true
    }

    override fun removeFavorite(userEmail: String, eventSlug: String): Boolean = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException("User with email $userEmail not found")
        val event = EventEntity.findBySlug(eventSlug) ?: return@transaction false
        if (!OrganisationPermissionEntity.hasPermission(event.organisation.id.value, user.id.value)) {
            throw ForbiddenException("You are not allowed to manage favorites for this event")
        }
        val favorite = FavoriteEventEntity.singleFavorite(user.id.value, event.id.value)
            ?: return@transaction false
        favorite.delete()
        true
    }
}
