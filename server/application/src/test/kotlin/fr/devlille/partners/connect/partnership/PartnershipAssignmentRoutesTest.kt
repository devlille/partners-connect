package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.insertLegalEntity
import fr.devlille.partners.connect.internal.insertMockCompany
import fr.devlille.partners.connect.internal.insertMockPartnership
import fr.devlille.partners.connect.internal.insertMockSponsoringPack
import fr.devlille.partners.connect.internal.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.internal.insertMockedUser
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipAssignmentRoutesTest {
    @Test
    fun `POST generates assignment PDF and returns URL`() = testApplication {
        val storage = mockk<Storage>()
        val expectedUrl = "https://example.com/generated.pdf"
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "assignment.pdf",
            url = expectedUrl,
        )

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
                selectedPack = insertMockSponsoringPack(eventId = eventId),
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/assignment") {
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
            filename = "assignment.pdf",
            url = "https://example.com/irrelevant.pdf",
        )

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/assignment") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when company does not exist`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "assignment.pdf",
            url = "https://example.com/irrelevant.pdf",
        )

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/assignment") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when partnership does not exist`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "assignment.pdf",
            url = "https://example.com/irrelevant.pdf",
        )

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockedEventWithAdminUser(eventId)
            insertMockCompany(companyId)
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/assignment") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 when legal entity representative is missing`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload("bucket", "file", "https://example.com")

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            val legalEntity = insertLegalEntity(representativeUser = insertMockedUser(name = null))
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId = eventId, legalEntity = legalEntity),
                company = insertMockCompany(companyId),
                selectedPack = insertMockSponsoringPack(eventId = eventId),
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            )
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/assignment") {
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
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
                validatedAt = null,
            )
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/assignment") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Validated pack not found"))
    }

    @Test
    fun `POST returns 500 when assignment template for language is missing`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload("bucket", "file", "https://example.com")

        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            insertMockPartnership(
                id = partnershipId,
                event = insertMockedEventWithAdminUser(eventId),
                company = insertMockCompany(companyId),
                selectedPack = insertMockSponsoringPack(eventId = eventId),
                validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                language = "xx",
            )
        }

        val response = client.post("/events/$eventId/companies/$companyId/partnership/$partnershipId/assignment") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains("Missing resource"))
    }
}
