package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedSupportVideo
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipSupportVideoSubmitRoutePostTest {
    @Test
    fun `POST uploads video to storage and returns 201 with id`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "support-video.mp4",
            url = "https://storage.googleapis.com/bucket/support-video.mp4",
        )

        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, orgId = orgId, slug = "dev-fest")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedCompany(companyId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/events/dev-fest/partnerships/$partnershipId/support-video",
            formData = formData {
                append(
                    "file",
                    File("src/test/resources/sample-support-video.mp4").readBytes(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "video/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=sample.mp4")
                    },
                )
            },
        )
        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.containsKey("id"))
    }

    @Test
    fun `POST returns 415 for unsupported video MIME type`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, orgId = orgId, slug = "dev-fest")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedCompany(companyId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }
        val response = client.submitFormWithBinaryData(
            url = "/events/dev-fest/partnerships/$partnershipId/support-video",
            formData = formData {
                append(
                    "file",
                    ByteArray(8) { 0 },
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=bogus.png")
                    },
                )
            },
        )
        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `POST returns 409 when prior video is APPROVED`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "support-video.mp4",
            url = "https://storage.googleapis.com/bucket/support-video.mp4",
        )
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        application {
            moduleMocked(mockStorage = module { single<Storage> { storage } })
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, orgId = orgId, slug = "dev-fest")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedCompany(companyId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedSupportVideo(
                    partnershipId = partnershipId,
                    eventId = eventId,
                    status = PromotionStatus.APPROVED,
                    reviewerUserId = userId,
                )
            }
        }
        val response = client.submitFormWithBinaryData(
            url = "/events/dev-fest/partnerships/$partnershipId/support-video",
            formData = formData {
                append(
                    "file",
                    ByteArray(8) { 0 },
                    Headers.build {
                        append(HttpHeaders.ContentType, "video/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=sample.mp4")
                    },
                )
            },
        )
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST returns 404 when event does not exist`() = testApplication {
        application { moduleSharedDb(userId = UUID.randomUUID()) }
        val response = client.submitFormWithBinaryData(
            url = "/events/missing/partnerships/${UUID.randomUUID()}/support-video",
            formData = formData {
                append(
                    "file",
                    ByteArray(8) { 0 },
                    Headers.build {
                        append(HttpHeaders.ContentType, "video/mp4")
                        append(HttpHeaders.ContentDisposition, "filename=sample.mp4")
                    },
                )
            },
        )
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
