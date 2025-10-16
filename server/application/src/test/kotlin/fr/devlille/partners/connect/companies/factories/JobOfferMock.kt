package fr.devlille.partners.connect.companies.factories

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedJobOffer(
    companyId: UUID,
    id: UUID = UUID.randomUUID(),
    url: String = "https://example.com/job-offer",
    title: String = "Kotlin Developer",
    location: String = "Remote",
    publicationDate: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    endDate: LocalDateTime? = null,
    experienceYears: Int? = null,
    salary: String? = null,
): CompanyJobOfferEntity = transaction {
    CompanyJobOfferEntity.new(id) {
        this.company = CompanyEntity.findById(companyId)!!
        this.url = url
        this.title = title
        this.location = location
        this.publicationDate = publicationDate
        this.endDate = endDate
        this.experienceYears = experienceYears ?: 0
        this.salary = salary
        this.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
