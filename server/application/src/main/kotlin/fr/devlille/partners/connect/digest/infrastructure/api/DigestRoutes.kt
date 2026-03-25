package fr.devlille.partners.connect.digest.infrastructure.api

import fr.devlille.partners.connect.digest.domain.DigestRepository
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject

fun Application.digestRoutes() {
    val digestRepository by inject<DigestRepository>()
    val notificationRepository by inject<NotificationRepository>()

    routing {
        post("/orgs/{orgSlug}/events/{eventSlug}/jobs/digest") {
            val eventSlug = call.parameters.eventSlug
            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            val digest = digestRepository.queryDigest(eventSlug, today)
            if (digest.hasItems) {
                val variables = NotificationVariables.MorningDigest(
                    language = call.request.headers[HttpHeaders.AcceptLanguage] ?: "en",
                    event = digest.event,
                    agreementItems = digest.agreementItems,
                    billingItems = digest.billingItems,
                    socialMediaItems = digest.socialMediaItems,
                    jobOfferItems = digest.jobOfferItems,
                )
                notificationRepository.sendMessageFromMessaging(variables)
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
