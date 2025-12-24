package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PartnershipCommunicationSupportRoutePutTest {
    @Test
    fun `PUT support upload uploads image and returns support URL`() = testApplication {
        val storage = mockk<Storage>()
        val expectedUrl = "https://storage.googleapis.com/bucket/partnerships/support.png"
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "support.png",
            url = expectedUrl,
        )

        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId, storage = storage)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val imageBytes = byteArrayOf(
            // PNG signature
            0x89.toByte(), 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
            // IHDR chunk
            0x00, 0x00, 0x00, 0x0D,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/communication/support") {
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
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val textContent = "This is not an image".toByteArray()

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/communication/support") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            setBody(textContent)
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertTrue(response.bodyAsText().contains("Unsupported image type"))
    }

    @Test
    fun `PUT support upload returns 400 for empty content`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/communication/support") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
            setBody(byteArrayOf())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("must not be empty"))
    }

    @Test
    fun `PUT support upload returns 401 when unauthorized`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val response = client.put("/orgs/test-org/events/$eventId/partnerships/$partnershipId/communication/support") {
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

        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId, storage = storage)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        // JPEG signature
        val imageBytes = byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0xFF.toByte(),
            0xE0.toByte(),
        )

        val response = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/communication/support") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
            setBody(imageBytes)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
