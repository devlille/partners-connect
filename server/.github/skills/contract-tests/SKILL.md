---
name: contract-tests
description: Use this skill when creating or modifying contract tests (unit tests) for Ktor API routes. Covers test class naming, location, shared database setup with moduleSharedDb, factory functions, HTTP client patterns, response assertions, authorization testing, schema validation, and status code coverage. Does NOT cover integration/end-to-end tests.
---

You are helping an engineer write contract tests for Ktor API routes in the partners-connect server. Contract tests validate the **HTTP contract** of a single endpoint — correct status codes, response structure, and request validation. They do NOT test end-to-end business workflows (that is the scope of integration tests).

## Test Location & Naming

### Location
Contract tests live in the `<feature>.infrastructure.api` package under `src/test/kotlin`:
```
application/src/test/kotlin/fr/devlille/partners/connect/<feature>/infrastructure/api/
```

### Naming Convention
**Pattern**: `<Feature><EndpointResource>Route<Verb>Test`

One test class per endpoint per HTTP verb. Examples:
- `CompanyJobOfferRouteGetTest` — Tests `GET /companies/{companyId}/job-offers/{jobOfferId}`
- `PromoteJobOfferRoutePostTest` — Tests `POST /companies/{companyId}/partnerships/{partnershipId}/promote`
- `CompanyUpdateRoutePutTest` — Tests `PUT /companies/{companyId}`
- `CompanySoftDeleteRouteDeleteTest` — Tests `DELETE /companies/{companyId}`
- `SponsoringPackCreationRoutePostTest` — Tests `POST /orgs/{orgSlug}/events/{eventSlug}/packs`
- `CompanyJobOfferRouteListTest` — Tests `GET /companies/{companyId}/job-offers` (list endpoint)
- `CompanyStatusFilterRouteGetTest` — Tests `GET /companies?filter[status]=active`

### Status Code Coverage (MANDATORY)
Every contract test class **MUST** test ALL status codes the endpoint can return:

| Status Code | When to Test |
|-------------|-------------|
| `200 OK` | Successful retrieval or update |
| `201 Created` | Successful resource creation |
| `204 No Content` | Successful deletion |
| `400 Bad Request` | Invalid input, schema validation failure, invalid UUID format |
| `401 Unauthorized` | Missing/invalid auth token, missing org permission (for `/orgs/` routes) |
| `403 Forbidden` | Authenticated but operation not allowed (e.g., past event) |
| `404 Not Found` | Resource doesn't exist, or wrong parent resource |
| `409 Conflict` | Business rule violation (e.g., duplicate promotion) |

## Test Structure

### Core Pattern
Every test method follows this structure:

```kotlin
@Test
fun `<VERB> <description> - returns <status code>`() = testApplication {
    // 1. Pre-create UUIDs
    val userId = UUID.randomUUID()
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()
    val companyId = UUID.randomUUID()

    // 2. Configure application with shared database
    application {
        moduleSharedDb(userId = userId)
        transaction {
            // 3. Insert ALL test data in a SINGLE transaction
            insertMockedOrganisationEntity(orgId)
            insertMockedFutureEvent(eventId, orgId = orgId)
            insertMockedCompany(companyId)
        }
    }

    // 4. Execute HTTP request
    val response = client.post("/events/$eventId/partnerships") {
        contentType(ContentType.Application.Json)
        setBody("""{"companyId":"$companyId"}""")
    }

    // 5. Assert status code and optionally response body
    assertEquals(HttpStatusCode.Created, response.status)
}
```

### Imports
```kotlin
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
```

## Shared Database Setup

### `moduleSharedDb`
All contract tests **MUST** use `moduleSharedDb(userId)`. This configures:
- A shared in-memory H2 database (`jdbc:h2:mem:test;DB_CLOSE_DELAY=-1`)
- Mocked HTTP engine (Google OAuth, BilletWeb)
- Mocked Slack, Storage, Geocoding, Billing, Webhooks via Koin

```kotlin
application {
    moduleSharedDb(userId = userId)
    transaction {
        // All data in ONE transaction
    }
}
```

**Parameters**:
- `userId: UUID` — The user ID associated with the mocked Google OAuth token. When a route calls `authRepository.getUserInfo(token)`, the mock engine returns a user info matching this user.
- `nbProductsForTickets: Int = 0` — Number of mock BilletWeb products (only needed for ticketing tests).
- `storage: Storage = mockk()` — Override storage mock if needed.
- `slack: Slack = mockk()` — Override Slack mock if needed.

### Data Initialization Rules

1. **Pre-create ALL UUIDs** before the `application {}` block:
```kotlin
val userId = UUID.randomUUID()
val orgId = UUID.randomUUID()
val eventId = UUID.randomUUID()
```

2. **Single `transaction {}` block** for all data:
```kotlin
transaction {
    insertMockedUser(userId)
    insertMockedOrganisationEntity(orgId)
    insertMockedOrgaPermission(orgId, userId = userId)
    insertMockedFutureEvent(eventId, orgId = orgId)
    insertMockedCompany(companyId)
}
```

3. **Never wrap factories in their own transactions** — factory functions must not manage transactions.

## Factory Functions

### Naming
- `insertMocked<Entity>()` — Creates and persists a database entity
- `create<Domain>()` — Creates a domain object (no database)

### Location
`<feature>/factories/<Name>.factory.kt` — e.g. `companies/factories/Company.factory.kt`

### Rules
- **All parameters MUST have defaults** — tests only specify what they need
- **Unique fields MUST use UUID-based defaults**: `name = id.toString()`
- **NO transaction management in factories**
- **`@Suppress("LongParameterList")`** on functions with many parameters

### Common Factories

| Factory | Package | Creates |
|---------|---------|---------|
| `insertMockedUser(userId)` | `users.factories` | User entity |
| `insertMockedOrganisationEntity(orgId)` | `organisations.factories` | Organisation |
| `insertMockedOrgaPermission(orgId, userId)` | `users.factories` | Org edit permission |
| `insertMockedFutureEvent(eventId, orgId)` | `events.factories` | Future event (submissions open) |
| `insertMockedPastEvent(eventId, orgId)` | `events.factories` | Past event (ended) |
| `insertMockedCompany(companyId)` | `companies.factories` | Company |
| `insertMockedJobOffer(companyId, jobOfferId)` | `companies.factories` | Job offer |
| `insertMockedSponsoringPack(packId, eventId)` | `sponsoring.factories` | Sponsoring pack |
| `insertMockedSponsoringOption(optionId, eventId)` | `sponsoring.factories` | Sponsoring option |
| `insertMockedPartnership(id, eventId, companyId)` | `partnership.factories` | Partnership |
| `insertMockedBilling(eventId, partnershipId)` | `partnership.factories` | Billing entry |
| `insertMockedSpeaker(speakerId, eventId)` | `agenda.factories` | Speaker entity |
| `createOrganisation()` | `organisations.factories` | Organisation domain object |
| `createSponsoringPack()` | `sponsoring.factories` | SponsoringPack domain object |
| `createEvent()` | `events.factories` | Event domain object |

### Entity Dependency Chain
Factories have implicit dependencies via foreign keys. Typical setup order:
```
1. insertMockedUser(userId)
2. insertMockedOrganisationEntity(orgId)
3. insertMockedOrgaPermission(orgId, userId)           // requires org + user
4. insertMockedFutureEvent(eventId, orgId = orgId)     // requires org
5. insertMockedCompany(companyId)
6. insertMockedSponsoringPack(packId, eventId)         // requires event
7. insertMockedPartnership(id, eventId, companyId)     // requires event + company
```

## HTTP Request Patterns

### GET Request
```kotlin
val response = client.get("/companies/$companyId/job-offers/$jobOfferId")
assertEquals(HttpStatusCode.OK, response.status)
```

### GET with Query Parameters
```kotlin
val response = client.get("/companies/$companyId/job-offers?page=2&page_size=5")
val response = client.get("/companies?filter[status]=active")
val response = client.get("/companies?query=search+term")
```

### POST Request (with JSON body)
```kotlin
val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
    contentType(ContentType.Application.Json)
    setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
}
assertEquals(HttpStatusCode.Created, response.status)
```

### POST Request (with raw JSON string)
```kotlin
val response = client.post("/events/$eventId/partnerships") {
    contentType(ContentType.Application.Json)
    setBody("""{"companyId":"$companyId", "packId":"$packId"}""")
}
```

### PUT Request
```kotlin
val response = client.put("/companies/$companyId") {
    contentType(ContentType.Application.Json)
    setBody(json.encodeToString(updateRequest))
}
assertEquals(HttpStatusCode.OK, response.status)
```

### DELETE Request
```kotlin
val response = client.delete("/companies/$companyId")
assertEquals(HttpStatusCode.NoContent, response.status)
```

### Authenticated Request (`/orgs/` routes)
```kotlin
val response = client.post("/orgs/$orgId/events/$eventId/packs") {
    contentType(ContentType.Application.Json)
    header(HttpHeaders.Authorization, "Bearer valid")
    setBody(json.encodeToString(createSponsoringPack()))
}
```

## Authorization Testing

### How the Mock Auth Works
- The mock engine intercepts Google OAuth requests
- `"Bearer valid"` (or any value NOT containing `"invalid"`) → returns a valid user info matching the `userId` passed to `moduleSharedDb(userId)`
- `"Bearer invalid"` → returns 401 Unauthorized from mock
- No `Authorization` header → `call.token` throws `UnauthorizedException`
- The `AuthorizedOrganisationPlugin` then checks `insertMockedOrgaPermission` for the resolved user

### Testing 401 — Missing Header
```kotlin
@Test
fun `POST returns 401 when Authorization header is missing`() = testApplication {
    val userId = UUID.randomUUID()
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()

    application {
        moduleSharedDb(userId)
        transaction {
            insertMockedUser(userId)
            insertMockedOrganisationEntity(orgId)
            insertMockedOrgaPermission(orgId, userId = userId)
            insertMockedFutureEvent(eventId, orgId = orgId)
        }
    }

    // No Authorization header
    val response = client.post("/orgs/$orgId/events/$eventId/packs") {
        contentType(ContentType.Application.Json)
        setBody("""{"name":"Test"}""")
    }

    assertEquals(HttpStatusCode.Unauthorized, response.status)
}
```

### Testing 401 — User Without Permission
```kotlin
@Test
fun `POST returns 401 when user lacks organization permissions`() = testApplication {
    val userId = UUID.randomUUID()
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()

    application {
        moduleSharedDb(userId)
        transaction {
            insertMockedUser(userId)
            insertMockedOrganisationEntity(orgId)
            // NO insertMockedOrgaPermission — user has no permission
            insertMockedFutureEvent(eventId, orgId = orgId)
        }
    }

    val response = client.post("/orgs/$orgId/events/$eventId/packs") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer valid")
        setBody("""{"name":"Test"}""")
    }

    assertEquals(HttpStatusCode.Unauthorized, response.status)
}
```

### Testing 401 — Invalid Token
```kotlin
@Test
fun `return 401 if token is expired or invalid`() = testApplication {
    val userId = UUID.randomUUID()
    application { moduleSharedDb(userId) }

    val response = client.get("/users/me/orgs") {
        header("Authorization", "Bearer invalid")
    }

    assertEquals(HttpStatusCode.Unauthorized, response.status)
}
```

## Response Body Assertions

### Pattern 1: JSON Object — Check Keys and Values
```kotlin
val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

assertTrue(body.containsKey("id"))
assertTrue(body.containsKey("company_id"))
assertEquals(jobOfferId.toString(), body["id"]?.jsonPrimitive?.content)
assertEquals(companyId.toString(), body["company_id"]?.jsonPrimitive?.content)
```

### Pattern 2: Typed Deserialization
```kotlin
private val json = Json { ignoreUnknownKeys = true }

val result = json.decodeFromString<PaginatedResponse<Company>>(response.bodyAsText())
assertTrue { result.total > 0 }
```

### Pattern 3: Created ID Extraction
```kotlin
val responseBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
assertNotNull(responseBody["id"], "Response should contain an 'id' field")
```

### Pattern 4: Paginated Response
```kotlin
val responseMap = Json.parseToJsonElement(response.bodyAsText()).jsonObject

assertTrue(responseMap.containsKey("items"))
assertTrue(responseMap.containsKey("page"))
assertTrue(responseMap.containsKey("page_size"))
assertTrue(responseMap.containsKey("total"))

val items = responseMap["items"]?.jsonArray
assertEquals(0, items!!.size)
assertEquals(2, responseMap["page"]?.jsonPrimitive?.content?.toInt())
assertEquals(5, responseMap["page_size"]?.jsonPrimitive?.content?.toInt())
```

### Pattern 5: Error Body Assertion
```kotlin
val errorBody = response.bodyAsText()
assert(errorBody.contains("pattern") || errorBody.contains("does not match") || errorBody.contains("Invalid"))
```

### Pattern 6: Verify Error Message from ResponseException
```kotlin
import fr.devlille.partners.connect.internal.infrastructure.api.ResponseException

val message = json.decodeFromString<ResponseException>(response.bodyAsText()).message
assertEquals("Request parameter 'name' is invalid: must not be empty", message)
```

## Schema Validation Testing (400)

### Missing Required Field
```kotlin
@Test
fun `POST returns 400 when required field is missing`() = testApplication {
    application { moduleSharedDb(userId = UUID.randomUUID()) }

    val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
        contentType(ContentType.Application.Json)
        setBody("""{}""")  // Missing required field
    }

    assertEquals(HttpStatusCode.BadRequest, response.status)
}
```

### Invalid Field Format
```kotlin
@Test
fun `POST returns 400 when field has invalid format`() = testApplication {
    application { moduleSharedDb(userId = UUID.randomUUID()) }

    val input = PromoteJobOfferRequest("invalid-uuid")
    val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
    }

    assertEquals(HttpStatusCode.BadRequest, response.status)
}
```

### Invalid Path Parameter
```kotlin
@Test
fun `GET with invalid UUID in path returns 400`() = testApplication {
    application { moduleSharedDb(userId = UUID.randomUUID()) }

    val response = client.get("/companies/$companyId/job-offers/invalid-uuid")

    assertEquals(HttpStatusCode.BadRequest, response.status)
}
```

## Complete Contract Test Examples

### Public Route — GET Single Resource
```kotlin
class CompanyJobOfferRouteGetTest {
    @Test
    fun `GET job offer by ID should return job offer with 200`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
                insertMockedJobOffer(companyId, jobOfferId)
            }
        }

        val response = client.get("/companies/$companyId/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(body.containsKey("id"))
        assertEquals(jobOfferId.toString(), body["id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET with invalid UUID should return 400`() = testApplication { /* ... */ }

    @Test
    fun `GET non-existent job offer should return 404`() = testApplication { /* ... */ }

    @Test
    fun `GET job offer for non-existent company should return 404`() = testApplication { /* ... */ }

    @Test
    fun `GET job offer from different company should return 404`() = testApplication { /* ... */ }
}
```

### Authenticated Route — POST with Full Coverage
```kotlin
class SponsoringPackCreationRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates a new pack`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/packs") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(createSponsoringPack()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST returns 401 when Authorization header is missing`() = testApplication {
        /* ... same setup, no header ... */
    }

    @Test
    fun `POST returns 401 when user lacks org permissions`() = testApplication {
        /* ... setup without insertMockedOrgaPermission ... */
    }

    @Test
    fun `POST returns 400 when body is invalid`() = testApplication {
        /* ... send invalid/empty JSON body ... */
    }
}
```

## Test File Organization

```
application/src/test/kotlin/fr/devlille/partners/connect/
├── companies/
│   ├── factories/
│   │   ├── Company.factory.kt
│   │   ├── JobOffer.factory.kt
│   │   └── CompanyJobOfferPromotion.factory.kt
│   └── infrastructure/
│       └── api/
│           ├── CompanyJobOfferRouteGetTest.kt
│           ├── CompanyJobOfferRouteListTest.kt
│           ├── CompanyJobOfferRouteCreateTest.kt
│           ├── CompanyJobOfferRouteUpdateTest.kt
│           ├── CompanyJobOfferRouteDeleteTest.kt
│           ├── CompanyUpdateRoutePutTest.kt
│           ├── CompanySoftDeleteRouteDeleteTest.kt
│           ├── CompanyListRouteGetTest.kt
│           ├── CompanyStatusFilterRouteGetTest.kt
│           ├── CompanyUploadLogoRoutePostTest.kt
│           ├── PromoteJobOfferRoutePostTest.kt
│           └── ListJobOfferPromotionsRouteGetTest.kt
├── events/
│   ├── factories/
│   │   ├── EventEntity.factory.kt
│   │   └── Event.factory.kt
│   └── infrastructure/
│       └── api/
│           └── EventCreationRoutePostTest.kt
├── partnership/
│   ├── factories/
│   │   ├── Partnership.factory.kt
│   │   ├── Billing.factory.kt
│   │   └── ...
│   └── infrastructure/
│       └── api/
│           ├── PartnershipBillingStatusRoutePostTest.kt
│           ├── PartnershipDecisionRoutePostTest.kt
│           └── ...
├── sponsoring/
│   ├── factories/
│   │   └── ...
│   └── infrastructure/
│       └── api/
│           └── SponsoringPackCreationRoutePostTest.kt
├── users/
│   ├── factories/
│   │   ├── UserEntity.factory.kt
│   │   └── EventPermissionEntity.factory.kt
│   └── infrastructure/
│       └── api/
│           ├── ListUsersRouteGetTest.kt
│           └── ListUserOrganisationsRouteGetTest.kt
└── internal/
    ├── ApplicationMock.kt        # moduleSharedDb / moduleMocked
    ├── HttpClientMockEngine.kt   # MockEngine configuration
    └── FakeBillingGateway.kt     # Billing test double
```

## Checklist for New Contract Tests

When writing contract tests for a new endpoint:

- [ ] Test class in `<feature>.infrastructure.api` package
- [ ] Class named `<Feature><Resource>Route<Verb>Test`
- [ ] One `@Test` per status code the endpoint returns
- [ ] `moduleSharedDb(userId)` in every test
- [ ] All test data in a single `transaction {}` block
- [ ] Pre-created UUIDs before `application {}` block
- [ ] For `/orgs/` routes: `insertMockedUser`, `insertMockedOrganisationEntity`, `insertMockedOrgaPermission`
- [ ] For `/orgs/` routes: test 401 without header, 401 without permission
- [ ] For POST/PUT: `contentType(ContentType.Application.Json)` and `setBody()`
- [ ] For `/orgs/` routes: `header(HttpHeaders.Authorization, "Bearer valid")`
- [ ] Assert response status code with `assertEquals(HttpStatusCode.*, response.status)`
- [ ] Assert response body structure for success cases
- [ ] Test 400 with missing required fields and invalid field formats
- [ ] Test 404 with non-existent resource IDs
