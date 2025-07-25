package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.MediaBinary
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.image.ImageProcessor
import java.io.File

class CompanyImageProcessingRepositoryDefault : CompanyImageProcessingRepository {
    override fun processSvg(file: File): MediaBinary = MediaBinary(
        mimeType = MimeType.SVG,
        original = file.readBytes(),
        png1000 = ImageProcessor.resizeSvg(file, width = 1000) ?: error("Failed to resize SVG to 1000px"),
        png500 = ImageProcessor.resizeSvg(file, width = 500) ?: error("Failed to resize SVG to 500px"),
        png250 = ImageProcessor.resizeSvg(file, width = 250) ?: error("Failed to resize SVG to 250px"),
    )

    override fun processImage(file: File): MediaBinary = MediaBinary(
        mimeType = MimeType.PNG,
        original = file.readBytes(),
        png1000 = ImageProcessor.resizeImage(file, width = 1000) ?: error("Failed to resize image to 1000px"),
        png500 = ImageProcessor.resizeImage(file, width = 500) ?: error("Failed to resize image to 500px"),
        png250 = ImageProcessor.resizeImage(file, width = 250) ?: error("Failed to resize image to 250px"),
    )
}
