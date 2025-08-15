package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipAgreementRoutesTest {
    @Test
    fun `POST generates agreement PDF and returns URL`() = testApplication {
        val storage = mockk<Storage>()
        val expectedUrl = "https://example.com/generated.pdf"
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "agreement.pdf",
            url = expectedUrl,
        )

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/agreement") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(expectedUrl))
    }

    @Test
    fun `POST returns 404 when event does not exist`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "agreement.pdf",
            url = "https://example.com/irrelevant.pdf",
        )

        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/agreement") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when company does not exist`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "agreement.pdf",
            url = "https://example.com/irrelevant.pdf",
        )

        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/agreement") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when partnership does not exist`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "agreement.pdf",
            url = "https://example.com/irrelevant.pdf",
        )

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/agreement") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when organisation representative is missing`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload("bucket", "file", "https://example.com")

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            val organisation = insertMockedOrganisationEntity(representativeUser = insertMockedUser(name = null))
            insertMockedEventWithAdminUser(eventId = eventId, organisation = organisation)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/agreement") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Representative not found"))
    }

    @Test
    fun `POST returns 404 when no validated pack is present`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload("bucket", "file", "https://example.com")

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                validatedAt = null,
            )
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/agreement") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Validated pack not found"))
    }

    @Test
    fun `POST returns 403 when agreement template for language is missing`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload("bucket", "file", "https://example.com")

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                language = "xx",
            )
        }

        val response = client.post("/events/$eventId/partnership/$partnershipId/agreement") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("Missing resource"))
    }

    @Test
    fun `POST upload signed agreement PDF and returns URL`() = testApplication {
        val storage = mockk<Storage>()
        val expectedUrl = "https://example.com/signed-generated.pdf"
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "signed-agreement.pdf",
            url = expectedUrl,
        )

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.submitFormWithBinaryData(
            url = "/events/$eventId/partnership/$partnershipId/signed-agreement",
            formData = formData {
                append(
                    "file",
                    "some unknown content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/pdf")
                        append(HttpHeaders.ContentDisposition, "filename=signed.pdf")
                    },
                )
            },
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(expectedUrl))
        val partnership = transaction { PartnershipEntity.singleByEventAndPartnership(eventId, partnershipId) }
        assertEquals(expectedUrl, partnership?.agreementSignedUrl)
    }

    @Test
    fun `POST upload signed agreement - returns 404 if partnership not found`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "signed-agreement.pdf",
            url = "https://example.com/signed-generated.pdf",
        )

        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        val response = client.submitFormWithBinaryData(
            url = "/events/$eventId/partnership/$partnershipId/signed-agreement",
            formData = formData {
                append(
                    "file",
                    "some content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/pdf")
                        append(HttpHeaders.ContentDisposition, "filename=signed.pdf")
                    },
                )
            },
        )

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST upload signed agreement - returns 400 if file part is missing`() = testApplication {
        val storage = mockk<Storage>()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.submitFormWithBinaryData(
            url = "/events/$eventId/partnership/$partnershipId/signed-agreement",
            formData = formData {
                // Intentionally not appending the file part to simulate the error
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Missing file part"))
    }

    @Test
    fun `POST upload signed agreement - returns 400 if content type is not PDF`() = testApplication {
        val storage = mockk<Storage>()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockedCompany(companyId)
            val selectedPack = insertMockedSponsoringPack(event = eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = selectedPack.id.value,
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.submitFormWithBinaryData(
            url = "/events/$eventId/partnership/$partnershipId/signed-agreement",
            formData = formData {
                append(
                    "file",
                    "<html>not a pdf</html>".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "text/html")
                        append(HttpHeaders.ContentDisposition, "filename=invalid.html")
                    },
                )
            },
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid file type"))
    }
}
