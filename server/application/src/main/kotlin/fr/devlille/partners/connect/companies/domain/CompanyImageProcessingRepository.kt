package fr.devlille.partners.connect.companies.domain

interface CompanyImageProcessingRepository {
    fun processSvg(bytes: ByteArray): MediaBinary

    fun processImage(bytes: ByteArray): MediaBinary
}
