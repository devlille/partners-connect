package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrgsPackFlyerTemplatePutRouteTest {
    private fun pngBytes(width: Int, height: Int): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        return out.toByteArray()
    }

    @Test
    fun `PUT uploads flyer template and zone`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "bucket",
            filename = "flyer-template.png",
            url = "https://storage.googleapis.com/bucket/flyer-template.png",
        )

        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId, storage = storage)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
            }
        }

        val template = pngBytes(1200, 800)
        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/packs/$packId/flyer-template",
            formData = formData {
                append(
                    "file",
                    template,
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=template.png")
                    },
                )
                append("zone", "{\"x\":100,\"y\":200,\"width\":800,\"height\":500}")
            },
        ) {
            method = HttpMethod.Put
            headers.append(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("flyer-template.png"))
    }

    @Test
    fun `PUT rejects non-PNG file with 415`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/packs/$packId/flyer-template",
            formData = formData {
                append(
                    "file",
                    ByteArray(10),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=t.jpg")
                    },
                )
                append("zone", "{\"x\":0,\"y\":0,\"width\":10,\"height\":10}")
            },
        ) {
            method = HttpMethod.Put
            headers.append(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `PUT rejects zone outside template bounds with 400`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
            }
        }

        val template = pngBytes(100, 100)
        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/packs/$packId/flyer-template",
            formData = formData {
                append(
                    "file",
                    template,
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=t.png")
                    },
                )
                append("zone", "{\"x\":50,\"y\":50,\"width\":80,\"height\":80}")
            },
        ) {
            method = HttpMethod.Put
            headers.append(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns 401 without auth`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/packs/$packId/flyer-template",
            formData = formData {
                append(
                    "file",
                    ByteArray(0),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=t.png")
                    },
                )
            },
        ) {
            method = HttpMethod.Put
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
