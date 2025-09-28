package fr.devlille.partners.connect.internal.infrastructure.ktor

import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.api.RequestBodyValidationException
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import io.github.optimumcode.json.schema.JsonSchemaLoader
import io.github.optimumcode.json.schema.SchemaType
import io.github.optimumcode.json.schema.ValidationError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import kotlinx.serialization.json.Json

val schemas by lazy {
    JsonSchemaLoader.create()
        .register(readResourceFile("/schemas/address.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/attach_options_to_pack.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/billing_contact.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/booth_location_request.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/booth_location_response.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/communication_item.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/communication_plan.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/company.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/company_billing_data.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/contact.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_by_identifiers.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_company.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_event_external_link.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_provider.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_sponsoring_option.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_sponsoring_pack.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/create_ticket_data.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/event.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/event_contact.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/event_display.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/event_external_link.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/event_summary.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/event_with_organisation.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/grant_permission_request.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/identifier.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/media.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/organisation.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/organisation_item.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/owner.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/paginated_event_summary.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/paginated_provider.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/partnership_item.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/provider.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/publication_date_request.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/publication_date_response.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/register_partnership.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/social.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/sponsoring_option.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/sponsoring_pack.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/suggest_partnership.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/support_upload_response.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/ticket.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/ticket_data.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/ticket_order.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/translated_label.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/user.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/user_info.schema.json"), SchemaType.DRAFT_7)
        .register(readResourceFile("/schemas/user_session.schema.json"), SchemaType.DRAFT_7)
}

suspend inline fun <reified T : Any> ApplicationCall.receive(schema: String): T = try {
    receive<T>()
} catch (ex: BadRequestException) {
    val schemaContent = readResourceFile("/schemas/$schema")
    val schema = schemas.fromDefinition(schemaContent, SchemaType.DRAFT_7)
    val body = receiveText()
    val errors = mutableListOf<ValidationError>()
    val valid = schema.validate(Json.parseToJsonElement(body), errors::add)
    if (valid) {
        throw ForbiddenException("Unreachable")
    } else {
        throw RequestBodyValidationException(
            errors = errors.map { it.message },
            message = "Request body does not conform to schema $schema",
            cause = ex,
        )
    }
}
