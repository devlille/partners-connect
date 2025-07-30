package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.domain.CompanyMediaRepository
import fr.devlille.partners.connect.companies.domain.Media
import fr.devlille.partners.connect.companies.domain.MediaBinary
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage

class CompanyMediaRepositoryGoogleCloud(
    private val storage: Storage,
) : CompanyMediaRepository {
    override fun upload(companyId: String, media: MediaBinary): Media = Media(
        original = storage.upload(
            filename = "companies/$companyId/original.${media.mimeType.extension}",
            content = media.original,
            mimeType = media.mimeType,
        ).url,
        png1000 = storage.upload(
            filename = "companies/$companyId/1000.png",
            content = media.png1000,
            mimeType = MimeType.PNG,
        ).url,
        png500 = storage.upload(
            filename = "companies/$companyId/500.png",
            content = media.png500,
            mimeType = MimeType.PNG,
        ).url,
        png250 = storage.upload(
            filename = "companies/$companyId/250.png",
            content = media.png250,
            mimeType = MimeType.PNG,
        ).url,
    )
}
