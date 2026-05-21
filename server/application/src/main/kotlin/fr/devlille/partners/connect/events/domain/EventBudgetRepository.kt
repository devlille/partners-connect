package fr.devlille.partners.connect.events.domain

interface EventBudgetRepository {
    /**
     * Returns aggregate budget totals and per-validated-pack breakdown for a single event.
     *
     * @param eventSlug Slug of the event whose budget is requested
     * @throws io.ktor.server.plugins.NotFoundException if the event is unknown
     */
    fun findByEventSlug(eventSlug: String): EventBudget
}
