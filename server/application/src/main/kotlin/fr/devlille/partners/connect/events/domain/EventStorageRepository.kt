package fr.devlille.partners.connect.events.domain

interface EventStorageRepository {
    fun uploadBoothPlanImage(
        eventSlug: String,
        content: ByteArray,
        mimeType: String,
    ): String
}
