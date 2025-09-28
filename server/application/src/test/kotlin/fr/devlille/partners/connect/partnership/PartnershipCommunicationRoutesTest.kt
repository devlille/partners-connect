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
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartnershipCommunicationRoutesTest {
    @Test
    fun `PUT publication date sets communication publication date and returns success`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-communication-pub"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId, "Test Company")
            insertMockedSponsoringPack(packId, eventId, "Test Pack")
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"publication_date": "2025-09-15T10:30:00"}"""

        val response = client.put(
            "/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/communication/publication",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), responseBody["id"]?.jsonPrimitive?.content)
        assertEquals("2025-09-15T10:30", responseBody["publication_date"]?.jsonPrimitive?.content)

        // Verify the database was updated
        val partnership = transaction {
            PartnershipEntity.singleByEventAndPartnership(eventId, partnershipId)
        }
        assertNotNull(partnership)
        assertEquals(
            LocalDateTime.parse("2025-09-15T10:30:00"),
            partnership.communicationPublicationDate,
        )
    }

    @Test
    fun `PUT publication date returns 400 for invalid date format`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-communication-invalid"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId, "Test Company")
            insertMockedSponsoringPack(packId, eventId, "Test Pack")
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val requestBody = """{"publication_date": "invalid-date"}"""

        val response = client.put(
            "/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/communication/publication",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid request body: value does not match 'date-time' format"))
    }

    @Test
    fun `PUT publication date returns 401 when unauthorized`() = testApplication {
        val eventSlug = "test-event-communication-unauth"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val requestBody = """{"publication_date": "2025-09-15T10:30:00"}"""

        val response = client.put(
            "/orgs/test-org/events/$eventSlug/partnership/$partnershipId/communication/publication",
        ) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT publication date returns 404 for non-existent partnership`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-communication-notfound"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val requestBody = """{"publication_date": "2025-09-15T10:30:00"}"""

        val response = client.put(
            "/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/communication/publication",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT support upload uploads image and returns support URL`() = testApplication {
        val storage = mockk<Storage>()
        val expectedUrl = "https://storage.googleapis.com/bucket/partnership/support.png"
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "support.png",
            url = expectedUrl,
        )

        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-communication-support"

        application {
            moduleMocked(
                mockStorage = module {
                    single<Storage> { storage }
                },
            )
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId, "Test Company")
            insertMockedSponsoringPack(packId, eventId, "Test Pack")
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val imageBytes = byteArrayOf(
            // PNG signature
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
            // IHDR chunk
            0x00, 0x00, 0x00, 0x0D,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/communication/support") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
            setBody(imageBytes)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), responseBody["id"]?.jsonPrimitive?.content)
        assertEquals(expectedUrl, responseBody["url"]?.jsonPrimitive?.content)

        // Verify the database was updated
        val partnership = transaction {
            PartnershipEntity.singleByEventAndPartnership(eventId, partnershipId)
        }
        assertNotNull(partnership)
        assertEquals(expectedUrl, partnership.communicationSupportUrl)

        // Verify the storage service was called
        verify { storage.upload(any(), imageBytes, any()) }
    }

    @Test
    fun `PUT support upload returns 415 for invalid image type`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-communication-invalid-type"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId, "Test Company")
            insertMockedSponsoringPack(packId, eventId, "Test Pack")
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val textContent = "This is not an image".toByteArray()

        val response = client.put("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/communication/support") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            setBody(textContent)
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertTrue(response.bodyAsText().contains("Unsupported image type"))
    }

    @Test
    fun `PUT support upload returns 400 for empty content`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-communication-empty"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId, "Test Company")
            insertMockedSponsoringPack(packId, eventId, "Test Pack")
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        val response = client.put("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/communication/support") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
            setBody(byteArrayOf())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Empty file content"))
    }

    @Test
    fun `PUT support upload returns 401 when unauthorized`() = testApplication {
        val eventSlug = "test-event-communication-support-unauth"
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val response = client.put(
            "/orgs/test-org/events/$eventSlug/partnership/$partnershipId/communication/support",
        ) {
            header(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
            setBody(imageBytes)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT support upload supports different image formats`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "support.jpg",
            url = "https://example.com/support.jpg",
        )

        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "test-event-communication-jpeg"

        application {
            moduleMocked(
                mockStorage = module {
                    single<Storage> { storage }
                },
            )
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId, "Test Company")
            insertMockedSponsoringPack(packId, eventId, "Test Pack")
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = packId,
            )
        }

        // JPEG signature
        val imageBytes = byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0xFF.toByte(),
            0xE0.toByte(),
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/partnership/$partnershipId/communication/support") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
            setBody(imageBytes)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
