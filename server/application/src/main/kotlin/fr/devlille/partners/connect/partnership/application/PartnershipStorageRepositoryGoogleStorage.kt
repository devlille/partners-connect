package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import java.util.UUID

class PartnershipStorageRepositoryGoogleStorage(
    private val storage: Storage,
) : PartnershipStorageRepository {
    override fun uploadAssignment(
        eventId: UUID,
        companyId: UUID,
        partnershipId: UUID,
        content: ByteArray,
    ): String {
        val uploaded = storage.upload(
            filename = "events/$eventId/partnerships/$partnershipId/assignment-$companyId.pdf",
            content = content,
            mimeType = MimeType.PDF,
        )
        return uploaded.url
    }
}
