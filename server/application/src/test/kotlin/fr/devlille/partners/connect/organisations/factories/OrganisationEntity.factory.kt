package fr.devlille.partners.connect.organisations.factories

import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedUser
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import kotlinx.datetime.Clock.System
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedOrganisationEntity(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    headOffice: String = "123 Test Street, Test City, TC 12345",
    siret: String? = null,
    siren: String? = null,
    tva: String? = null,
    dAndB: String? = null,
    nace: String? = null,
    naf: String? = null,
    duns: String? = null,
    iban: String = "FR76 1234 5678 9012 3456 7890 1234",
    bic: String = "TESTFRPPXXX",
    ribUrl: String = "https://example.com/rib/test-organisation",
    creationLocation: String = "Test City, TC 12345",
    createdAt: LocalDateTime = System.now().toLocalDateTime(TimeZone.UTC),
    publishedAt: LocalDateTime = System.now().toLocalDateTime(TimeZone.UTC),
    representativeUser: UserEntity = insertMockedUser(),
    representativeRole: String = "Test Representative Role",
): OrganisationEntity = OrganisationEntity.new(id) {
    this.name = name
    this.slug = name.slugify()
    this.headOffice = headOffice
    this.siret = siret
    this.siren = siren
    this.tva = tva
    this.dAndB = dAndB
    this.nace = nace
    this.naf = naf
    this.duns = duns
    this.iban = iban
    this.bic = bic
    this.ribUrl = ribUrl
    this.creationLocation = creationLocation
    this.createdAt = createdAt
    this.publishedAt = publishedAt
    this.representativeUser = representativeUser
    this.representativeRole = representativeRole
}
