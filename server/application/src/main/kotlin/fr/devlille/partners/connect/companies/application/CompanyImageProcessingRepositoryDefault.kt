package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.MediaBinary
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.image.ImageProcessor

class CompanyImageProcessingRepositoryDefault : CompanyImageProcessingRepository {
    override fun processSvg(bytes: ByteArray): MediaBinary = MediaBinary(
        mimeType = MimeType.SVG,
        original = bytes,
        png1000 = ImageProcessor.resizeSvg(bytes, width = WIDTH_LARGE) ?: error("Failed to resize SVG to 1000px"),
        png500 = ImageProcessor.resizeSvg(bytes, width = WIDTH_MEDIUM) ?: error("Failed to resize SVG to 500px"),
        png250 = ImageProcessor.resizeSvg(bytes, width = WIDTH_SMALL) ?: error("Failed to resize SVG to 250px"),
    )

    override fun processImage(bytes: ByteArray): MediaBinary {
        val resized = ImageProcessor.resizeImageToWidths(bytes, listOf(WIDTH_LARGE, WIDTH_MEDIUM, WIDTH_SMALL))
        return MediaBinary(
            mimeType = MimeType.PNG,
            original = bytes,
            png1000 = resized.getValue(WIDTH_LARGE),
            png500 = resized.getValue(WIDTH_MEDIUM),
            png250 = resized.getValue(WIDTH_SMALL),
        )
    }

    companion object {
        private const val WIDTH_LARGE = 1000
        private const val WIDTH_MEDIUM = 500
        private const val WIDTH_SMALL = 250
    }
}
