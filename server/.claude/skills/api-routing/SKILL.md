---
name: api-routing
description: Use this skill when creating, modifying, or reviewing Ktor API routes in the partners-connect server. Covers route structure, plugin installation, /orgs vs public paths, StringValues extensions, StatusPages exceptions, notification plugins, and schema validation patterns.
---

You are helping an engineer implement or modify Ktor API routes in the partners-connect server. Follow these patterns exactly — they are validated against the existing codebase.

## Route File Structure

### Location & Naming
- Route files live in `<domain>/infrastructure/api/<Feature>Routes.kt`
- Each domain exposes a single top-level function `fun Route.<domain>Routes()` that mounts all sub-routes
- Sub-routes are split into private functions: `private fun Route.publicPartnershipRoutes()` and `private fun Route.orgsPartnershipRoutes()`
- The top-level route function is registered in `App.kt` inside the `routing {}` block

### Mount Point in App.kt
```kotlin
routing {
    authRoutes { redirects[it] }
    companyRoutes()
    organisationRoutes()
    eventRoutes()
    userRoutes()
    sponsoringRoutes()
    partnershipRoutes()
    integrationRoutes()
    providersRoutes()
    digestRoutes()
}
```

## Two Route Categories: Public vs Authenticated (`/orgs`)

### Public Routes (NO `/orgs` prefix)
Public routes are accessible without organization-level authorization. They do NOT install `AuthorizedOrganisationPlugin`.

```kotlin
private fun Route.publicPartnershipRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()

    route("/events/{eventSlug}/partnerships") {
        // No plugin installed — public access
        post {
            val eventSlug = call.parameters.eventSlug
            val register = call.receive<RegisterPartnership>(schema = "register_partnership.schema.json")
            val id = partnershipRepository.register(eventSlug, register)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
    }
}
```

Common public path patterns:
- `/events/{eventSlug}/...` — Public event data
- `/companies` and `/companies/{companyId}` — Company CRUD
- `/providers` — Provider listing
- `/auth/...` — Authentication endpoints

### Authenticated Routes (`/orgs/{orgSlug}/...`)
Routes under `/orgs/{orgSlug}/...` **MUST** install `AuthorizedOrganisationPlugin`. This plugin:
1. Extracts JWT token from `Authorization` header via `call.token`
2. Validates token and retrieves user info via `AuthRepository.getUserInfo()`
3. Checks `canEdit` permission for the organization
4. Throws `UnauthorizedException` if the user lacks permission
5. Stores the user permission in `call.attributes.user`

```kotlin
private fun Route.orgsPartnershipRoutes() {
    val repository by inject<PartnershipRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships") {
        install(AuthorizedOrganisationPlugin) // REQUIRED

        get {
            val eventSlug = call.parameters.eventSlug
            val partnerships = repository.findAll(eventSlug)
            call.respond(HttpStatusCode.OK, partnerships)
        }
    }
}
```

**NEVER** do manual permission checking when `AuthorizedOrganisationPlugin` is installed:
```kotlin
// ❌ WRONG — duplicates plugin functionality
post {
    val userInfo = authRepository.getUserInfo(call.token)
    if (!userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)) {
        throw UnauthorizedException("Not allowed")
    }
}

// ✅ CORRECT — plugin handles everything before route code runs
post {
    // Plugin guarantees user has canEdit=true
    val eventSlug = call.parameters.eventSlug
    // ... business logic
}
```

### Accessing Authenticated User
After `AuthorizedOrganisationPlugin` runs, the user permission is available via:
```kotlin
import fr.devlille.partners.connect.internal.infrastructure.ktor.user

val currentUser = call.attributes.user // UserOrganisationPermission
val userId = call.attributes.user.userId.toUUID()
```

## Available Route-Scoped Plugins

Install plugins inside `route {}` blocks using `install(PluginName)`.

### 1. `AuthorizedOrganisationPlugin`
**Purpose**: Authorization for `/orgs/{orgSlug}/...` routes.
**Package**: `fr.devlille.partners.connect.internal.infrastructure.ktor`
**Behavior**: Validates JWT token + org permission. Throws `UnauthorizedException` on failure.

### 2. `NotificationPartnershipPlugin`
**Purpose**: Sends email notifications after a successful response and records email history.
**Package**: `fr.devlille.partners.connect.internal.infrastructure.ktor`
**Trigger**: `onCallRespond` — executes after `call.respond()` succeeds.
**Usage**: Set `call.attributes.variables` with `NotificationVariables` before calling `call.respond()`.

```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/validate") {
    install(AuthorizedOrganisationPlugin)
    install(NotificationPartnershipPlugin)  // Sends email after respond
    install(WebhookPartnershipPlugin)       // Fires webhook after respond

    post {
        val eventSlug = call.parameters.eventSlug
        val partnershipId = call.parameters.partnershipId
        val partnership = partnershipRepository.getById(eventSlug, partnershipId)

        // Build notification variables BEFORE responding
        val variables = NotificationVariables.PartnershipValidated(
            language = partnership.language,
            event = eventRepository.getBySlug(eventSlug),
            company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
            partnership = partnership,
            pack = partnership.selectedPack
                ?: throw ForbiddenException("Partnership does not have a selected pack"),
        )
        call.attributes.variables = variables  // Plugin reads this on respond
        call.respond(HttpStatusCode.OK, mapOf("id" to id.toString()))
    }
}
```

### 3. `MessagingPartnershipPlugin`
**Purpose**: Sends Slack/messaging notifications after a successful response.
**Package**: `fr.devlille.partners.connect.internal.infrastructure.ktor`
**Trigger**: `onCallRespond` — same pattern as `NotificationPartnershipPlugin`.
**Usage**: Same `call.attributes.variables` pattern.

### 4. `WebhookPartnershipPlugin`
**Purpose**: Fires partnership webhooks after a successful response.
**Package**: `fr.devlille.partners.connect.internal.infrastructure.ktor`
**Trigger**: `onCallRespond` — reads `eventSlug` and `partnershipId` from path parameters.
**Usage**: Just install it — no explicit attribute setup needed.

### Plugin Combination Pattern
Multiple plugins can be installed on the same route. They execute in order on `onCallRespond`:
```kotlin
route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/validate") {
    install(AuthorizedOrganisationPlugin)    // Auth check (onCall)
    install(NotificationPartnershipPlugin)   // Email (onCallRespond)
    install(WebhookPartnershipPlugin)        // Webhook (onCallRespond)

    post { /* ... */ }
}
```

## StringValues Extensions

**NEVER** extract path parameters manually. Use the predefined `StringValues` extension properties:

```kotlin
// ✅ CORRECT
val eventSlug = call.parameters.eventSlug
val partnershipId = call.parameters.partnershipId

// ❌ WRONG — manual null check
val eventSlug = call.parameters["eventSlug"]
    ?: throw IllegalArgumentException("Missing eventSlug")
```

### Core Helper
All extensions use `getValue()` from `internal/infrastructure/api/StringValues.ex.kt`, which throws `MissingRequestParameterException` (mapped to 400 Bad Request by StatusPages):
```kotlin
fun StringValues.getValue(name: String): String = this[name]
    ?: throw MissingRequestParameterException(parameterName = name)
```

### Available Extensions by Domain

| Extension | Type | Path Parameter | Package |
|-----------|------|---------------|---------|
| `orgSlug` | `String` | `{orgSlug}` | `organisations.infrastructure.api` |
| `eventSlug` | `String` | `{eventSlug}` | `events.infrastructure.api` |
| `linkUUID` | `UUID` | `{linkId}` | `events.infrastructure.api` |
| `partnershipId` | `UUID` | `{partnershipId}` | `partnership.infrastructure.api` |
| `activityId` | `UUID` | `{activityId}` | `partnership.infrastructure.api` |
| `communicationPlanId` | `UUID` | `{id}` | `partnership.infrastructure.api` |
| `ticketId` | `String` | `{ticketId}` | `partnership.infrastructure.api` |
| `billingStatus` | `InvoiceStatus` | `{billingStatus}` | `partnership.infrastructure.api` |
| `companyUUID` | `UUID` | `{companyId}` | `companies.infrastructure.api` |
| `jobOfferUUID` | `UUID` | `{jobOfferId}` | `companies.infrastructure.api` |
| `packId` | `UUID` | `{packId}` | `sponsoring.infrastructure.api` |
| `optionId` | `UUID` | `{optionId}` | `sponsoring.infrastructure.api` |
| `providerId` | `UUID` | `{providerId}` | `provider.infrastructure.api` |
| `speakerId` | `UUID` | `{speakerId}` | `agenda.infrastructure.api` |

### `call.token` Extension
Extracts the JWT token from the `Authorization` header or session cookie:
```kotlin
import fr.devlille.partners.connect.internal.infrastructure.api.token

val ApplicationCall.token: String
    get() = request.headers["Authorization"]
        ?: sessions.get<UserSession>()?.let { "Bearer ${it.token}" }
        ?: throw UnauthorizedException("Token is missing from session or headers")
```

### Creating New Extensions
When adding a new path parameter, create an extension in the domain's `infrastructure/api/StringValues.ext.kt`:
```kotlin
package fr.devlille.partners.connect.<domain>.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.getValue
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.util.StringValues
import java.util.UUID

val StringValues.myEntityId: UUID
    get() = getValue("myEntityId").toUUID()
```

### Query Parameter Utilities
For boolean query parameters, use the `toBooleanStrict` helper:
```kotlin
import fr.devlille.partners.connect.partnership.infrastructure.api.toBooleanStrict

val includeDeclined = call.request.queryParameters["filter[include_declined]"]
    .toBooleanStrict("filter[include_declined]", default = false)
```

## StatusPages Exception Handling

The `StatusPages` plugin in `App.kt` maps exceptions to HTTP responses automatically. **NEVER** use try-catch in routes — throw exceptions and let StatusPages handle them.

### Exception → HTTP Status Code Mapping

| Exception | HTTP Status | Package |
|-----------|------------|---------|
| `BadRequestException` | 400 | `io.ktor.server.plugins` (Ktor built-in) |
| `RequestBodyValidationException` | 400 (with error list) | `internal.infrastructure.api` |
| `MissingRequestParameterException` | 400 | `io.ktor.server.plugins` (Ktor built-in) |
| `ValidationException` | 400 | `internal.infrastructure.api` |
| `EmptyStringValidationException` | 400 | `internal.infrastructure.api` |
| `EmptyListValidationException` | 400 | `internal.infrastructure.api` |
| `URLValidationException` | 400 | `internal.infrastructure.api` |
| `MustBePositiveValidationException` | 400 | `internal.infrastructure.api` |
| `MissingRequestHeaderException` | 400 | `internal.infrastructure.api` |
| `UnauthorizedException` | 401 | `internal.infrastructure.api` |
| `ForbiddenException` | 403 | `internal.infrastructure.api` |
| `NotFoundException` | 404 | `io.ktor.server.plugins` (Ktor built-in) |
| `ConflictException` | 409 | `internal.infrastructure.api` |
| `UnsupportedMediaTypeException` | 415 | `internal.infrastructure.api` |
| `Throwable` (catch-all) | 500 | (any uncaught exception) |

### Custom Exceptions (from `internal.infrastructure.api`)
```kotlin
// 401 — Authentication/authorization failure
throw UnauthorizedException("You are not allowed to edit this event")

// 403 — Forbidden (authenticated but not allowed)
throw ForbiddenException("Partnership does not have a selected pack")

// 409 — Business rule conflict
throw ConflictException("Provider is still attached to events and cannot be deleted")

// 415 — Wrong content type
throw UnsupportedMediaTypeException("Expected multipart/form-data")
```

### Ktor Built-in Exceptions
```kotlin
// 400 — Bad request (general)
throw BadRequestException("Company status '$it' is invalid")

// 404 — Not found
throw NotFoundException("Partnership not found")
```

### Validation Exceptions (400 subtypes)
```kotlin
throw EmptyStringValidationException("name")       // "Request parameter 'name' is invalid: must not be empty"
throw EmptyListValidationException("options")       // "Request parameter 'options' is invalid: must not be an empty list"
throw URLValidationException("website")             // "Request parameter 'website' is invalid: must be a valid URL"
throw MustBePositiveValidationException("quantity")  // "Request parameter 'quantity' is invalid: must be a positive number"
throw MissingRequestHeaderException("Accept-Language") // "Request header Accept-Language is missing"
```

### Response Format
All error responses use the `ResponseException` DTO:
```json
{
  "message": "Error description",
  "errors": ["optional", "error", "list"],
  "stack": "optional stack trace (dev only)"
}
```

## JSON Schema Validation

Use `call.receive<T>(schema)` for request body validation — **NEVER** validate manually:

```kotlin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive

post {
    val input = call.receive<CreateCompany>(schema = "create_company.schema.json")
    // Validation already done — proceed with business logic
    val id = companyRepository.createOrUpdate(input)
    call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
}
```

Schema files live in `application/src/main/resources/schemas/` and are registered in the `schemas` lazy val in `ApplicationCall.ext.kt`.

If validation fails, `RequestBodyValidationException` is thrown automatically → 400 with error details.

## Response Patterns

```kotlin
// 200 — Success with data
call.respond(HttpStatusCode.OK, data)

// 201 — Created with ID
call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))

// 201 — Created with full entity
call.respond(HttpStatusCode.Created, provider)

// 204 — No content (delete operations)
call.respond(HttpStatusCode.NoContent)
```

## Dependency Injection

Repositories are injected via Koin at the route function level (NOT inside individual handlers):

```kotlin
private fun Route.orgsPartnershipRoutes() {
    // ✅ Inject at route function level
    val partnershipRepository by inject<PartnershipRepository>()
    val eventRepository by inject<EventRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships") {
        install(AuthorizedOrganisationPlugin)
        get { /* use repositories here */ }
        post { /* use repositories here */ }
    }
}
```

## Complete Route Template

Use this as a starting point for new routes:

```kotlin
package fr.devlille.partners.connect.<domain>.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.<domain>Routes() {
    public<Domain>Routes()
    orgs<Domain>Routes()
}

private fun Route.public<Domain>Routes() {
    val repository by inject<<Domain>Repository>()

    route("/events/{eventSlug}/<resources>") {
        get {
            val eventSlug = call.parameters.eventSlug
            val result = repository.findByEvent(eventSlug)
            call.respond(HttpStatusCode.OK, result)
        }

        post {
            val eventSlug = call.parameters.eventSlug
            val input = call.receive<Create<Domain>>(schema = "create_<domain>.schema.json")
            val id = repository.create(eventSlug, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
    }
}

private fun Route.orgs<Domain>Routes() {
    val repository by inject<<Domain>Repository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/<resources>") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug
            val result = repository.findByEvent(eventSlug)
            call.respond(HttpStatusCode.OK, result)
        }

        post {
            val eventSlug = call.parameters.eventSlug
            val input = call.receive<Create<Domain>>(schema = "create_<domain>.schema.json")
            val id = repository.create(eventSlug, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }
    }
}
```
