---
name: integration-tests
description: 'End-to-end integration tests that exercise complete workflows through multiple HTTP routes. Use when writing tests that validate business logic across endpoints, state transitions, cascading operations, or cross-domain interactions. Covers test structure, shared database setup, multi-step workflows, response parsing, database state verification, and authorization testing. NOT for contract/unit tests (single-endpoint schema validation).'
---

# Integration Tests — End-to-End Workflow Testing

## Overview

Integration tests validate **complete business workflows** by chaining multiple HTTP calls and verifying state transitions end-to-end. They live in the **feature root package** under `application/src/test/kotlin/fr/devlille/partners/connect/<feature>/` — NOT in the `infrastructure/api/` subpackage (that's for contract tests).

Each test class covers a specific workflow or scenario. A single `@Test` method often exercises multiple routes in sequence: create → verify → update → verify → delete → verify.

---

## 1 — File Location & Naming

| Aspect | Rule |
|---|---|
| **Package** | `fr.devlille.partners.connect.<feature>` (root of the domain) |
| **Naming** | `<Feature>(<EndpointResource>)RoutesTest` — always plural **"Routes"** |
| **Location** | `application/src/test/kotlin/fr/devlille/partners/connect/<feature>/` |

Examples:
- `PartnershipSpeakersRoutesTest` — speaker attach/detach workflow
- `PartnershipDeletionRoutesTest` — deletion with cascading verification
- `CompanyJobOfferRoutesTest` — job offer CRUD lifecycle
- `ProvidersAttachmentRoutesTest` — provider attach/detach across events
- `BoothActivityRoutesTest` — full CRUD lifecycle (POST → GET → PUT → DELETE)
- `PartnershipDeclinedFilterRoutesTest` — filter combination AND logic
- `DigestJobRoutesTest` — job trigger with Slack notification verification

---

## 2 — Test Structure (MANDATORY)

Every integration test follows this exact skeleton:

```kotlin
package fr.devlille.partners.connect.<feature>

import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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

class <Feature><Resource>RoutesTest {

    @Test
    fun `descriptive workflow name`() = testApplication {
        // 1. Pre-create ALL UUIDs
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        // 2. Configure shared DB + seed data in ONE transaction
        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                // ... minimal data for this workflow
            }
        }

        // 3. Multi-step HTTP workflow with assertions at each step
        // Step 1: Create
        val createResponse = client.post("/...") { /* ... */ }
        assertEquals(HttpStatusCode.Created, createResponse.status)

        // Step 2: Verify creation
        val getResponse = client.get("/...")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        // Assert response body contains expected data

        // Step 3: Update / Delete / Next action
        // ...

        // Step 4: Verify final state
        // ...
    }
}
```

### Rules

- **ALWAYS** use `testApplication { }` from Ktor test framework.
- **ALWAYS** pre-create ALL UUIDs before `application { }` block.
- **ALWAYS** use `moduleSharedDb(userId)` — NEVER `moduleMocked()`.
- **ALWAYS** seed ALL test data in a **single** `transaction { }` block inside `application { }`.
- **ALWAYS** use factory functions (`insertMocked*()`) — never raw SQL or direct entity creation.
- **ALWAYS** annotate long test methods with `@Suppress("LongMethod")`.
- **NEVER** extend a base test class — tests are self-contained.
- **NEVER** share state between `@Test` methods — each test sets up its own data.

---

## 3 — Shared Database Setup

### moduleSharedDb

All integration tests use a shared in-memory H2 database:

```kotlin
application {
    moduleSharedDb(userId = userId)
}
```

The function signature:

```kotlin
fun Application.moduleSharedDb(
    userId: UUID,
    nbProductsForTickets: Int = 0,   // BilletWeb mock: number of ticket products
    storage: Storage = mockk(),       // Google Cloud Storage mock
    slack: Slack = mockk(),           // Slack client mock
)
```

- `userId` — the authenticated user for the mock OAuth engine. `"Bearer valid"` tokens resolve to this user.
- Pass a real `slack` mock when testing Slack notification workflows (see section 8).
- Pass `nbProductsForTickets` when testing BilletWeb ticket generation.

### Data Seeding

Seed **only** the data necessary for the workflow. Common seeding pattern for org-scoped routes:

```kotlin
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
        validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    )
}
```

### Factory Function Rules Recap

- Named `insertMocked<Entity>()` — creates and persists in DB.
- All parameters have defaults.
- Unique fields default to `id.toString()`.
- **NO** `transaction { }` inside factories — the caller wraps everything.

---

## 4 — HTTP Client Usage

### Public Routes (no auth)

```kotlin
val response = client.get("/events/$eventId/partnerships/$partnershipId")
assertEquals(HttpStatusCode.OK, response.status)
```

### Org-Scoped Routes (auth required)

```kotlin
val response = client.get("/orgs/$orgId/events/$eventId/partnerships") {
    header(HttpHeaders.Authorization, "Bearer valid")
    header(HttpHeaders.Accept, "application/json")
}
assertEquals(HttpStatusCode.OK, response.status)
```

`"Bearer valid"` is recognised by the mock engine and resolves to the `userId` passed to `moduleSharedDb`.

### POST with JSON Body

```kotlin
val response = client.post("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId") {
    contentType(ContentType.Application.Json)
    setBody("""{"title":"Demo","description":"A booth demo"}""")
}
assertEquals(HttpStatusCode.Created, response.status)
```

### POST with Serialized Object

```kotlin
val input = listOf(providerId1.toString(), providerId2.toString())
val response = client.post("/orgs/$orgId/events/$eventId/providers") {
    header("Authorization", "Bearer valid")
    contentType(ContentType.Application.Json)
    setBody(json.encodeToString(input))
}
```

### PUT

```kotlin
val response = client.put("$baseUrl/$activityId") {
    contentType(ContentType.Application.Json)
    setBody("""{"title":"Updated Demo","description":"Updated description"}""")
}
assertEquals(HttpStatusCode.OK, response.status)
```

### DELETE

```kotlin
val response = client.delete("/orgs/$orgId/events/$eventId/partnerships/$partnershipId") {
    header(HttpHeaders.Authorization, "Bearer valid")
}
assertEquals(HttpStatusCode.NoContent, response.status)
```

---

## 5 — Response Parsing

### 5.1 JSON Element Parsing (Most Common)

```kotlin
val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
val id = body["id"]?.jsonPrimitive?.content
val speakers = body["speakers"]?.jsonArray!!
assertEquals(1, speakers.size)
assertEquals(speakerId.toString(), speakers[0].jsonObject["id"]?.jsonPrimitive?.content)
```

### 5.2 Array Response

```kotlin
val items = Json.parseToJsonElement(response.bodyAsText()).jsonArray
assertEquals(2, items.size)
assertTrue(items.any { it.jsonObject["title"]?.jsonPrimitive?.content == "Expected Title" })
```

### 5.3 Typed Deserialization

```kotlin
private val json = Json { ignoreUnknownKeys = true }

val result = json.decodeFromString<PaginatedResponse<PartnershipItem>>(response.bodyAsText())
assertEquals(2, result.total)
assertEquals(2, result.items.size)
assertTrue(result.items.any { it.id == targetId.toString() })
```

### 5.4 Map Extraction (for create responses)

```kotlin
val createdMap = json.decodeFromString<Map<String, String>>(response.bodyAsText())
val newId = createdMap["id"]!!
```

### Rules

- Declare `private val json = Json { ignoreUnknownKeys = true }` as a class property when using typed deserialization.
- Prefer `Json.parseToJsonElement()` for ad-hoc field checks.
- Prefer typed deserialization (`decodeFromString<T>()`) for paginated responses or complex DTOs.

---

## 6 — Multi-Step Workflow Patterns

### 6.1 Full CRUD Lifecycle

Test the complete Create → Read → Update → Read → Delete → Read cycle:

```kotlin
@Test
@Suppress("LongMethod")
fun `full CRUD lifecycle for booth activities`() = testApplication {
    // ... setup ...

    // POST: create
    val createResponse = client.post(baseUrl) {
        contentType(ContentType.Application.Json)
        setBody("""{"title":"First Demo","description":"A description"}""")
    }
    assertEquals(HttpStatusCode.Created, createResponse.status)
    val created = Json.parseToJsonElement(createResponse.bodyAsText()).jsonObject
    val activityId = created["id"]!!.jsonPrimitive.content

    // GET: verify created
    val listResponse = client.get(baseUrl)
    val items = Json.parseToJsonElement(listResponse.bodyAsText()).jsonArray
    assertEquals(1, items.size)

    // PUT: update
    val updateResponse = client.put("$baseUrl/$activityId") {
        contentType(ContentType.Application.Json)
        setBody("""{"title":"Updated Demo"}""")
    }
    assertEquals(HttpStatusCode.OK, updateResponse.status)

    // GET: verify updated
    val listResponse2 = client.get(baseUrl)
    val items2 = Json.parseToJsonElement(listResponse2.bodyAsText()).jsonArray
    assertTrue(items2.any { it.jsonObject["title"]?.jsonPrimitive?.content == "Updated Demo" })

    // DELETE: remove
    val deleteResponse = client.delete("$baseUrl/$activityId")
    assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

    // GET: verify deleted
    val listResponse3 = client.get(baseUrl)
    val items3 = Json.parseToJsonElement(listResponse3.bodyAsText()).jsonArray
    assertEquals(0, items3.size)
}
```

### 6.2 Attach / Detach Workflow

```kotlin
@Test
fun `complete attach-detach workflow`() = testApplication {
    // ... setup ...

    // Step 1: Attach
    val attachResponse = client.post("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId") {
        contentType(ContentType.Application.Json)
    }
    assertEquals(HttpStatusCode.Created, attachResponse.status)

    // Step 2: Verify attached
    val getResponse = client.get("/events/$eventId/partnerships/$partnershipId")
    val body = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
    assertEquals(1, body["speakers"]?.jsonArray!!.size)

    // Step 3: Detach
    val detachResponse = client.delete("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId")
    assertEquals(HttpStatusCode.NoContent, detachResponse.status)

    // Step 4: Verify detached
    val finalResponse = client.get("/events/$eventId/partnerships/$partnershipId")
    val finalBody = Json.parseToJsonElement(finalResponse.bodyAsText()).jsonObject
    assertEquals(0, finalBody["speakers"]?.jsonArray!!.size)
}
```

### 6.3 Idempotency / Conflict Testing

```kotlin
@Test
fun `duplicate attachment returns conflict`() = testApplication {
    // ... setup ...

    // First attachment succeeds
    val first = client.post("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId") {
        contentType(ContentType.Application.Json)
    }
    assertEquals(HttpStatusCode.Created, first.status)

    // Second attachment fails with 409 Conflict
    val second = client.post("/events/$eventId/partnerships/$partnershipId/speakers/$speakerId") {
        contentType(ContentType.Application.Json)
    }
    assertEquals(HttpStatusCode.Conflict, second.status)
}
```

### 6.4 Consistency Under Repeated Operations

```kotlin
@Test
fun `data stays consistent after repeated attach-detach cycles`() = testApplication {
    // ... setup ...

    repeat(3) {
        val attach = client.post("/.../speakers/$speakerId") { contentType(ContentType.Application.Json) }
        assertEquals(HttpStatusCode.Created, attach.status)

        val detach = client.delete("/.../speakers/$speakerId")
        assertEquals(HttpStatusCode.NoContent, detach.status)
    }

    // Verify clean state after cycles
    val final = client.get("/.../partnerships/$partnershipId")
    val speakers = Json.parseToJsonElement(final.bodyAsText()).jsonObject["speakers"]?.jsonArray!!
    assertEquals(0, speakers.size)
}
```

---

## 7 — Database State Verification

When HTTP responses alone aren't enough (e.g., verifying cascading deletes), query the database directly in a `transaction { }` block **after** the HTTP call:

```kotlin
// Delete partnership
val deleteResponse = client.delete("/orgs/$orgId/events/$eventId/partnerships/$partnershipId") {
    header(HttpHeaders.Authorization, "Bearer valid")
}
assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

// Verify cascade in DB
transaction {
    val emails = PartnershipEmailEntity
        .find { PartnershipEmailsTable.partnershipId eq partnershipId }
        .count()
    assertEquals(0, emails, "All emails should be deleted")

    val tickets = PartnershipTicketEntity
        .find { PartnershipTicketsTable.partnershipId eq partnershipId }
        .count()
    assertEquals(0, tickets, "All tickets should be deleted")

    val speakers = SpeakerPartnershipEntity
        .find { SpeakerPartnershipTable.partnershipId eq partnershipId }
        .count()
    assertEquals(0, speakers, "All speaker links should be deleted")

    val billing = BillingEntity
        .find { BillingsTable.partnershipId eq partnershipId }
        .count()
    assertEquals(0, billing, "All billing records should be deleted")
}
```

You can also verify **before** the action to confirm preconditions:

```kotlin
application {
    moduleSharedDb(userId = userId)
    transaction {
        // ... seed data ...
    }

    // Verify preconditions
    transaction {
        val count = PartnershipEmailEntity
            .find { PartnershipEmailsTable.partnershipId eq partnershipId }
            .count()
        assertEquals(2, count, "Should have 2 emails before delete")
    }
}
```

### Rules

- Use `Entity.find { condition }.count()` for counting records.
- Use `Entity.findById(id)` + `assertNotNull`/`assertNull` for single-record checks.
- Always add assertion messages to clarify intent: `assertEquals(0, count, "All emails should be deleted")`.
- Keep `transaction { }` blocks **outside** the `application { }` block when verifying post-HTTP-call state.

---

## 8 — Testing Slack Notifications

Use a mocked Slack client to verify notification side effects:

```kotlin
private fun slackMock(): Triple<Slack, MethodsClient, ChatPostMessageResponse> {
    val slack = mockk<Slack>()
    val slackMethod = mockk<MethodsClient>()
    val slackResponse = mockk<ChatPostMessageResponse>()
    every { slack.methods(any()) } returns slackMethod
    every {
        slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>())
    } returns slackResponse
    every { slackResponse.isOk } returns true
    return Triple(slack, slackMethod, slackResponse)
}

@Test
fun `digest job triggers Slack notification`() = testApplication {
    val userId = UUID.randomUUID()
    val (slack, slackMethod, _) = slackMock()

    application {
        moduleSharedDb(userId = userId, slack = slack)
        transaction { /* ... seed data ... */ }
    }

    val response = client.post("/orgs/$orgId/events/$eventId/jobs/digest")
    assertEquals(HttpStatusCode.NoContent, response.status)

    // Verify Slack was called
    verify { slackMethod.chatPostMessage(any<RequestConfigurator<ChatPostMessageRequest.ChatPostMessageRequestBuilder>>()) }
}
```

---

## 9 — Filter & Pagination Testing

### Filter Combination

```kotlin
@Test
fun `filters combine with AND logic`() = testApplication {
    // ... setup partnerships with various states ...

    val response = client.get("/orgs/$orgId/events/$eventId/partnerships?filter[declined]=true&filter[validated]=true") {
        header(HttpHeaders.Authorization, "Bearer valid")
    }
    assertEquals(HttpStatusCode.OK, response.status)

    val body = json.decodeFromString<PaginatedResponse<PartnershipItem>>(response.bodyAsText())
    assertEquals(1, body.total) // Only the partnership matching BOTH filters
}
```

### Pagination

```kotlin
val response = client.get("/companies/$companyId/job-offers?page=1&page_size=10")
val body = json.decodeFromString<PaginatedResponse<JobOfferResponse>>(response.bodyAsText())
assertEquals(expectedTotal, body.total)
assertEquals(expectedPageSize, body.items.size)
```

---

## 10 — HTTP Status Codes in Integration Tests

| Status | When to expect | Verify after |
|---|---|---|
| `201 Created` | POST creates a new resource | Create action |
| `200 OK` | GET returns data, or PUT returns updated resource | Read/Update action |
| `204 NoContent` | DELETE succeeds, or job trigger completes | Delete/Trigger action |
| `404 NotFound` | Resource does not exist (or user lacks permission) | GET after delete, invalid ID |
| `409 Conflict` | Duplicate create or business rule violation | Second identical POST |
| `403 Forbidden` | User has auth but action is denied by business rule | Cross-org access attempt |
| `400 BadRequest` | Invalid input parameters | Bad query params |

---

## 11 — Test File Tree

```
application/src/test/kotlin/fr/devlille/partners/connect/
├── <feature>/
│   ├── <Feature><Resource>RoutesTest.kt      # Integration tests (THIS skill)
│   ├── factories/
│   │   └── <Entity>.factory.kt               # Test data factories
│   └── infrastructure/
│       └── api/
│           └── <Feature>Route<Verb>Test.kt    # Contract tests (separate skill)
```

---

## Anti-Patterns (NEVER DO)

| Anti-pattern | Correct approach |
|---|---|
| Multiple `transaction { }` blocks for seeding | Single `transaction { }` wrapping all factory calls |
| `moduleMocked()` instead of `moduleSharedDb()` | Use `moduleSharedDb(userId)` always |
| Hardcoded names/identifiers in factories | UUID-based defaults: `name = id.toString()` |
| `transaction { }` inside factory functions | Caller manages the transaction |
| Sharing mutable state between `@Test` methods | Each test creates its own data from scratch |
| Testing a single endpoint status code only | Chain multiple routes to test the full workflow |
| Skipping verification after mutation | Always GET after POST/PUT/DELETE to confirm state |
| Extending a common base test class | Each test is self-contained — no inheritance |
| Using `assertNotNull` on response without status check | Check `assertEquals(HttpStatusCode.OK, ...)` first |
