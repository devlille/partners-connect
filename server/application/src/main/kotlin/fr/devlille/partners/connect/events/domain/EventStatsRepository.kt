package fr.devlille.partners.connect.events.domain

interface EventStatsRepository {
    /**
     * Returns per-partner statistics for every non-declined partnership of the event.
     *
     * @param eventSlug Slug of the event whose partner stats are requested
     * @throws io.ktor.server.plugins.NotFoundException if the event is unknown
     */
    fun findByEventSlug(eventSlug: String): EventStats
}
