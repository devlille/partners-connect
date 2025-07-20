package fr.devlille.partners.connect.companies.domain

import java.io.File

interface CompanyImageProcessingRepository {
    fun processSvg(companyId: String, file: File): MediaBinary

    fun processImage(companyId: String, file: File): MediaBinary
}
