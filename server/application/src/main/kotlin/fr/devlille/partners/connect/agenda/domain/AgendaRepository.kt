package fr.devlille.partners.connect.agenda.domain

interface AgendaRepository {
    /**
     * Fetches agenda data from OpenPlanner and stores it in the database
     * @param eventSlug The slug of the event to fetch agenda for
     */
    fun fetchAndStore(eventSlug: String)

    /**
     * Retrieves complete agenda data including sessions, speakers, and their partnerships
     * @param eventSlug The slug of the event to get agenda for
     * @return Complete agenda response with sessions and speakers including partnership data
     */
    fun getAgendaByEventSlug(eventSlug: String): AgendaResponse
}
