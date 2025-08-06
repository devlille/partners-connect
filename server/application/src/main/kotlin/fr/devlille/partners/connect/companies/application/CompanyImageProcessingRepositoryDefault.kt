package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.MediaBinary
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.image.ImageProcessor

class CompanyImageProcessingRepositoryDefault : CompanyImageProcessingRepository {
    override fun processSvg(bytes: ByteArray): MediaBinary = MediaBinary(
        mimeType = MimeType.SVG,
        original = bytes,
        png1000 = ImageProcessor.resizeSvg(bytes, width = 1000) ?: error("Failed to resize SVG to 1000px"),
        png500 = ImageProcessor.resizeSvg(bytes, width = 500) ?: error("Failed to resize SVG to 500px"),
        png250 = ImageProcessor.resizeSvg(bytes, width = 250) ?: error("Failed to resize SVG to 250px"),
    )

    override fun processImage(bytes: ByteArray): MediaBinary = MediaBinary(
        mimeType = MimeType.PNG,
        original = bytes,
        png1000 = ImageProcessor.resizeImage(bytes, width = 1000) ?: error("Failed to resize image to 1000px"),
        png500 = ImageProcessor.resizeImage(bytes, width = 500) ?: error("Failed to resize image to 500px"),
        png250 = ImageProcessor.resizeImage(bytes, width = 250) ?: error("Failed to resize image to 250px"),
    )
}
