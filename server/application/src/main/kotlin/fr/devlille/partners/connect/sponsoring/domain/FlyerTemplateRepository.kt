package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface FlyerTemplateRepository {
    /**
     * Returns the configured flyer template for the given pack, or null when the pack is not flyer-enabled.
     */
    fun get(eventSlug: String, packId: UUID): FlyerTemplate?

    /**
     * Persists the PNG template bytes into storage and writes the five flyer columns on the pack.
     * Replaces the existing template file if one is configured. Throws if the pack does not belong to the event.
     */
    fun save(eventSlug: String, packId: UUID, pngBytes: ByteArray, zone: FlyerZone): FlyerTemplate

    /**
     * Clears all five flyer columns on the pack and deletes the underlying template file.
     * No-op if the pack is not flyer-enabled.
     */
    fun clear(eventSlug: String, packId: UUID)
}
