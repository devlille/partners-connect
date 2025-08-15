package fr.devlille.partners.connect.legaentity.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.legaentity.domain.LegalEntity
import fr.devlille.partners.connect.legaentity.domain.LegalEntityRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.legalEntityRoutes() {
    val repository by inject<LegalEntityRepository>()
    val authRepository by inject<AuthRepository>()

    route("/legal-entities") {
        post {
            val input = call.receive<LegalEntity>()
            val id = repository.create(input)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: throw BadRequestException("Invalid or missing ID")
            call.respond(HttpStatusCode.OK, repository.getById(id))
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toUUID() ?: throw BadRequestException("Invalid or missing ID")
            val token = call.token
            val userInfo = authRepository.getUserInfo(token)
            
            // Get the existing legal entity to check ownership
            val existingEntity = repository.getById(id)
            
            // Check if the authenticated user is the representative user
            if (existingEntity.representativeUserEmail != userInfo.email) {
                throw UnauthorizedException("You are not allowed to edit this legal entity")
            }
            
            val updatedEntity = call.receive<LegalEntity>()
            val result = repository.update(id, updatedEntity)
            call.respond(HttpStatusCode.OK, result)
        }
    }
}
