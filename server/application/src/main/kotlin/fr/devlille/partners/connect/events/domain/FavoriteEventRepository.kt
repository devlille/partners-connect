package fr.devlille.partners.connect.events.domain

interface FavoriteEventRepository {
    /**
     * @return the caller's favorites, ordered by event start_time ascending. Favorites whose
     *   event belongs to an organisation the caller has no permission on are filtered out.
     */
    fun listByUserEmail(userEmail: String): List<EventSummary>

    /**
     * @return true if a new favorite row was inserted, false if it already existed.
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException if the event slug is unknown.
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException if the event belongs to an org the caller has no permission on.
     */
    fun addFavorite(userEmail: String, eventSlug: String): Boolean

    /**
     * @return true if a favorite row was deleted, false if no favorite existed (covers both unknown event and known-but-not-favorited).
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException if the event exists but belongs to an org the caller has no permission on.
     */
    fun removeFavorite(userEmail: String, eventSlug: String): Boolean
}
