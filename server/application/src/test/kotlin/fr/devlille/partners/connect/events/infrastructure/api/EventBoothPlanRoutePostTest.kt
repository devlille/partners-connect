package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.internal.infrastructure.bucket.Upload
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBoothPlanRoutePostTest {
    @Test
    fun `POST booth plan uploads image and returns URL`() = testApplication {
        val storage = mockk<Storage>()
        every { storage.upload(any(), any(), any()) } returns Upload(
            bucketName = "test-bucket",
            filename = "booth-plan.png",
            url = "https://example.com/booth-plan.png",
        )

        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId, storage = storage)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/booth-plan",
            formData = formData {
                append(
                    "file",
                    "fake PNG content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=booth-plan.png")
                    },
                )
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("https://example.com/booth-plan.png"))
    }

    @Test
    fun `POST booth plan returns 400 when no file is provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/booth-plan",
            formData = formData {
                // No file appended
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Request parameter file is missing"))
    }

    @Test
    fun `POST booth plan returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/booth-plan",
            formData = formData {
                append(
                    "file",
                    "fake PNG content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=booth-plan.png")
                    },
                )
            },
        )

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST booth plan returns 404 for non-existent event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/non-existent-event/booth-plan",
            formData = formData {
                append(
                    "file",
                    "fake PNG content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=booth-plan.png")
                    },
                )
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST booth plan returns 415 when rejects non-image file`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/booth-plan",
            formData = formData {
                append(
                    "file",
                    "some text content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=document.txt")
                    },
                )
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Unsupported media type"))
    }

    @Test
    fun `POST booth plan returns 415 when rejects unsupported image type`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.submitFormWithBinaryData(
            url = "/orgs/$orgId/events/$eventId/booth-plan",
            formData = formData {
                append(
                    "file",
                    "fake TIFF content".toByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentType, "image/tiff")
                        append(HttpHeaders.ContentDisposition, "filename=booth-plan.tiff")
                    },
                )
            },
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Unsupported media type"))
    }
}
