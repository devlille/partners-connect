package fr.devlille.partners.connect.sponsoring.domain

/**
 * Repository interface for public access to sponsoring packages by event.
 *
 * Provides read-only access to sponsoring packages for public consumption,
 * separate from the authenticated organizational management interface.
 *
 * @see PackRepository for authenticated organizational access
 */
interface EventPackRepository {
    /**
     * Retrieves all sponsoring packages for a public event by slug.
     *
     * This method provides public access to sponsoring packages without authentication,
     * including both required (embedded) and optional add-on options with proper
     * internationalization support.
     *
     * @param eventSlug The unique event identifier used in public URLs
     * @param language Language code from Accept-Language header for option translations (e.g., "en", "fr")
     * @return List of sponsoring packages with localized option names and descriptions
     * @throws NotFoundException if event with the specified slug does not exist
     *
     * @see SponsoringPack for the returned data structure
     * @see SponsoringOption for option details and translations
     */
    fun findPublicPacksByEvent(eventSlug: String, language: String): List<SponsoringPack>
}
