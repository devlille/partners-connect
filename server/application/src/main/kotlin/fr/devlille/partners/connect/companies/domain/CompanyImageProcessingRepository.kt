package fr.devlille.partners.connect.companies.domain

import java.io.File

interface CompanyImageProcessingRepository {
    fun processSvg(file: File): MediaBinary

    fun processImage(file: File): MediaBinary
}
