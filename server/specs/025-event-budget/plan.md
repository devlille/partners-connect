# Event Budget Endpoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /orgs/{orgSlug}/events/{eventSlug}/budget` that returns aggregate budget totals (paid / validated / total + diffs) plus a per-validated-pack breakdown listing each partnership's company name and effective `price_applied`.

**Architecture:** A new `EventBudgetRepository{,Exposed}` and `EventBudgetRoutes` under the existing `events/` module, mirroring the established `EventStats` flow (same DI pattern, same `AuthorizedOrganisationPlugin`). Pricing is computed by a pure helper that reads pre-loaded entities — no N+1.

**Tech Stack:** Kotlin / Ktor / Exposed (JDBC) / Koin / kotlinx.serialization / OpenAPI via Redocly bundle.

---

## File Structure

**Create:**
- `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventBudget.kt` — serializable DTOs (`EventBudget`, `BudgetTotals`, `PackBudget`, `PartnershipBudgetItem`).
- `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventBudgetRepository.kt` — interface.
- `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetRepositoryExposed.kt` — Exposed implementation.
- `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetPricing.kt` — pure helper functions: `pricingPack()` extension + `computeOptionEffectivePrice()`.
- `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventBudgetRoutes.kt` — Ktor route registration.
- `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt` — contract tests.
- `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetLifecycleRoutesTest.kt` — end-to-end integration test.
- `application/src/main/resources/schemas/event_budget.schema.json`
- `application/src/main/resources/schemas/budget_totals.schema.json`
- `application/src/main/resources/schemas/pack_budget.schema.json`
- `application/src/main/resources/schemas/partnership_budget_item.schema.json`

**Modify:**
- `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/bindings/EventModule.kt` — register Koin binding.
- `application/src/main/kotlin/fr/devlille/partners/connect/App.kt` — wire `eventBudgetRoutes()`.
- `application/src/main/resources/openapi/openapi.yaml` — add path + 4 component refs.

---

## Task 1: Domain DTOs and repository interface

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventBudget.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventBudgetRepository.kt`

- [ ] **Step 1: Create `EventBudget.kt`**

```kotlin
package fr.devlille.partners.connect.events.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventBudget(
    val currency: String,
    val totals: BudgetTotals,
    val packs: List<PackBudget>,
)

@Serializable
data class BudgetTotals(
    val paid: Int,
    val validated: Int,
    @SerialName("validated_minus_paid")
    val validatedMinusPaid: Int,
    val total: Int,
    @SerialName("total_minus_validated")
    val totalMinusValidated: Int,
)

@Serializable
data class PackBudget(
    @SerialName("pack_id")
    val packId: String,
    @SerialName("pack_name")
    val packName: String,
    @SerialName("base_price")
    val basePrice: Int,
    val partnerships: List<PartnershipBudgetItem>,
)

@Serializable
data class PartnershipBudgetItem(
    @SerialName("partnership_id")
    val partnershipId: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("price_applied")
    val priceApplied: Int,
)
```

- [ ] **Step 2: Create `EventBudgetRepository.kt`**

```kotlin
package fr.devlille.partners.connect.events.domain

interface EventBudgetRepository {
    /**
     * Returns aggregate budget totals and per-validated-pack breakdown for a single event.
     *
     * @param eventSlug Slug of the event whose budget is requested
     * @throws io.ktor.server.plugins.NotFoundException if the event is unknown
     */
    fun findByEventSlug(eventSlug: String): EventBudget
}
```

- [ ] **Step 3: Compile**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL — files compile (no usages yet).

- [ ] **Step 4: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventBudget.kt application/src/main/kotlin/fr/devlille/partners/connect/events/domain/EventBudgetRepository.kt
git commit -m "feat(server): add EventBudget DTOs and repository interface"
```

---

## Task 2: Pricing helper (pure functions)

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetPricing.kt`

- [ ] **Step 1: Create `EventBudgetPricing.kt`**

This helper mirrors the option-pricing rules in `PartnershipOptionEntity.ext.kt:43-127` so totals never diverge from `PartnershipPack.totalPrice`.

```kotlin
package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity

/**
 * Pricing pack used to value a partnership for the budget endpoint.
 * Order: validated → suggestion → selected. Null when no pack is chosen yet.
 */
fun PartnershipEntity.pricingPack(): SponsoringPackEntity? =
    validatedPack() ?: suggestionPack ?: selectedPack

/**
 * Effective price for a single PartnershipOptionEntity.
 *
 * Mirrors `PartnershipOptionEntity.toDomain(...).totalPrice` in partnership/application/mappers.
 * Defined here as a pure function over the entity so callers can pre-load options in batch
 * and compute totals in memory without re-issuing per-row queries.
 */
fun PartnershipOptionEntity.effectivePrice(): Int = when (option.optionType) {
    OptionType.TEXT -> priceOverride ?: option.price ?: 0
    OptionType.TYPED_QUANTITATIVE -> (priceOverride ?: option.price ?: 0) * (selectedQuantity ?: 0)
    OptionType.TYPED_NUMBER -> (priceOverride ?: option.price ?: 0) * (option.fixedQuantity ?: 0)
    OptionType.TYPED_SELECTABLE -> priceOverride ?: selectedValue?.price ?: 0
}
```

- [ ] **Step 2: Compile**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetPricing.kt
git commit -m "feat(server): add EventBudget pricing helper (pricingPack + effectivePrice)"
```

---

## Task 3: Failing route test — empty event returns zeros

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt`

- [ ] **Step 1: Create the test file with a single failing case**

```kotlin
package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBudgetRoutesTest {
    @Test
    fun `returns zero totals and empty packs when event has no partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("EUR", body["currency"]!!.jsonPrimitive.content)
        val totals = body["totals"]!!.jsonObject
        assertEquals(0, totals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, totals["total_minus_validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, body["packs"]!!.jsonArray.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest.returns zero totals and empty packs when event has no partnerships"`
Expected: FAIL — route not registered → 404 Not Found (likely a non-200 status code assertion failure).

- [ ] **Step 3: Create the repository implementation skeleton**

Create `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetRepositoryExposed.kt`:

```kotlin
package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.BudgetTotals
import fr.devlille.partners.connect.events.domain.EventBudget
import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EventBudgetRepositoryExposed : EventBudgetRepository {
    override fun findByEventSlug(eventSlug: String): EventBudget = transaction {
        EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        EventBudget(
            currency = "EUR",
            totals = BudgetTotals(
                paid = 0,
                validated = 0,
                validatedMinusPaid = 0,
                total = 0,
                totalMinusValidated = 0,
            ),
            packs = emptyList(),
        )
    }
}
```

- [ ] **Step 4: Create the route file**

Create `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventBudgetRoutes.kt`:

```kotlin
package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.eventBudgetRoutes() {
    val repository by inject<EventBudgetRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/budget") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug
            val budget = repository.findByEventSlug(eventSlug)
            call.respond(HttpStatusCode.OK, budget)
        }
    }
}
```

- [ ] **Step 5: Register the Koin binding**

Edit `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/bindings/EventModule.kt`. Add the import and the binding:

```kotlin
package fr.devlille.partners.connect.events.infrastructure.bindings

import fr.devlille.partners.connect.events.application.EventBudgetRepositoryExposed
import fr.devlille.partners.connect.events.application.EventRepositoryExposed
import fr.devlille.partners.connect.events.application.EventStatsRepositoryExposed
import fr.devlille.partners.connect.events.application.EventStorageRepositoryGoogleStorage
import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventStatsRepository
import fr.devlille.partners.connect.events.domain.EventStorageRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.koin.dsl.module

val eventModule = module {
    single<EventRepository> {
        EventRepositoryExposed(EventEntity)
    }
    single<EventStorageRepository> {
        EventStorageRepositoryGoogleStorage(get())
    }
    single<EventStatsRepository> {
        EventStatsRepositoryExposed()
    }
    single<EventBudgetRepository> {
        EventBudgetRepositoryExposed()
    }
}
```

- [ ] **Step 6: Wire the route in `App.kt`**

Edit `application/src/main/kotlin/fr/devlille/partners/connect/App.kt`. Add the import next to the other `events.infrastructure.api` imports:

```kotlin
import fr.devlille.partners.connect.events.infrastructure.api.eventBudgetRoutes
```

And add the call inside the `routing { ... }` block, right after `eventExternalLinkRoutes()`:

```kotlin
        eventBoothPlanRoutes()
        eventExternalLinkRoutes()
        eventBudgetRoutes()
```

(Also note: `eventStatsRoutes()` is currently NOT registered in `App.kt` despite the test class existing — leave that alone, this plan only adds the budget route.)

Wait — re-verify before editing. Read `App.kt` and confirm `eventStatsRoutes()` IS or IS NOT registered. If it IS, add `eventBudgetRoutes()` after it. If it is not, add right after `eventExternalLinkRoutes()`. Either way, the new line is one of:

```kotlin
        eventStatsRoutes()
        eventBudgetRoutes()
```

or

```kotlin
        eventExternalLinkRoutes()
        eventBudgetRoutes()
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest.returns zero totals and empty packs when event has no partnerships"`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetRepositoryExposed.kt application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/api/EventBudgetRoutes.kt application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/bindings/EventModule.kt application/src/main/kotlin/fr/devlille/partners/connect/App.kt application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt
git commit -m "feat(server): scaffold GET /orgs/{org}/events/{event}/budget returning zeros"
```

---

## Task 4: Compute totals across paid / validated / total

**Files:**
- Modify: `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetRepositoryExposed.kt`

- [ ] **Step 1: Add the failing test for mixed lifecycle states**

Append to `EventBudgetRoutesTest.kt`:

```kotlin
    @Test
    @Suppress("LongMethod")
    fun `sums priceApplied per lifecycle state and excludes declined`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()
        val goldPackId = UUID.randomUUID()

        val paidCompanyId = UUID.randomUUID()
        val validatedCompanyId = UUID.randomUUID()
        val pendingCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()

        val paidPartnershipId = UUID.randomUUID()
        val validatedPartnershipId = UUID.randomUUID()
        val pendingPartnershipId = UUID.randomUUID()
        val declinedPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
                fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack(
                    id = goldPackId,
                    eventId = eventId,
                    name = "Gold",
                    basePrice = 1000,
                )

                fr.devlille.partners.connect.companies.factories.insertMockedCompany(paidCompanyId, name = "Acme")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(validatedCompanyId, name = "Beta")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(pendingCompanyId, name = "Gamma")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(declinedCompanyId, name = "Delta")

                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = paidPartnershipId,
                    eventId = eventId,
                    companyId = paidCompanyId,
                    selectedPackId = goldPackId,
                )
                fr.devlille.partners.connect.partnership.factories.insertMockedBilling(
                    eventId = eventId,
                    partnershipId = paidPartnershipId,
                    status = fr.devlille.partners.connect.partnership.domain.InvoiceStatus.PAID,
                )

                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = validatedPartnershipId,
                    eventId = eventId,
                    companyId = validatedCompanyId,
                    selectedPackId = goldPackId,
                )

                // Pending (not yet validated) with a selectedPack — should be in totals.total only
                fr.devlille.partners.connect.partnership.factories.insertMockedPartnership(
                    id = pendingPartnershipId,
                    eventId = eventId,
                    companyId = pendingCompanyId,
                    selectedPackId = goldPackId,
                )

                fr.devlille.partners.connect.partnership.factories.insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = goldPackId,
                    declinedAt = kotlinx.datetime.LocalDateTime.parse("2024-01-01T00:00:00"),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val totals = Json.parseToJsonElement(response.bodyAsText()).jsonObject["totals"]!!.jsonObject
        assertEquals(1000, totals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(2000, totals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(3000, totals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["total_minus_validated"]!!.jsonPrimitive.content.toInt())
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest.sums priceApplied per lifecycle state and excludes declined"`
Expected: FAIL — totals all `0`.

- [ ] **Step 3: Replace `EventBudgetRepositoryExposed.kt` with the totals-aware implementation**

```kotlin
package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.BudgetTotals
import fr.devlille.partners.connect.events.domain.EventBudget
import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.events.domain.PackBudget
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EventBudgetRepositoryExposed : EventBudgetRepository {
    override fun findByEventSlug(eventSlug: String): EventBudget = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value

        val partnerships = PartnershipEntity
            .filters(
                eventId = eventId,
                packId = null,
                validated = null,
                suggestion = null,
                agreementGenerated = null,
                agreementSigned = null,
                organiserUserId = null,
                declined = false,
            )
            .orderBy(PartnershipsTable.createdAt to SortOrder.ASC)
            .toList()

        if (partnerships.isEmpty()) {
            return@transaction EventBudget(
                currency = "EUR",
                totals = BudgetTotals(0, 0, 0, 0, 0),
                packs = emptyList(),
            )
        }

        val partnershipIds = partnerships.map { it.id.value }.toSet()

        val paidPartnershipIds: Set<UUID> = BillingEntity
            .find {
                (BillingsTable.eventId eq eventId) and
                    (BillingsTable.partnershipId inList partnershipIds) and
                    (BillingsTable.status eq InvoiceStatus.PAID)
            }
            .map { it.partnership.id.value }
            .toSet()

        val optionsByPartnership: Map<UUID, List<PartnershipOptionEntity>> = PartnershipOptionEntity
            .find { PartnershipOptionsTable.partnershipId inList partnershipIds }
            .toList()
            .groupBy { it.partnership.id.value }

        val pricingPackIds = partnerships.mapNotNull { it.pricingPack()?.id?.value }.toSet()
        val requiredOptionIdsByPack: Map<UUID, Set<UUID>> = if (pricingPackIds.isEmpty()) {
            emptyMap()
        } else {
            PackOptionsTable
                .selectAll()
                .where { PackOptionsTable.pack inList pricingPackIds }
                .toList()
                .filter { it[PackOptionsTable.required] }
                .groupBy({ it[PackOptionsTable.pack].value }, { it[PackOptionsTable.option].value })
                .mapValues { (_, v) -> v.toSet() }
        }

        val priceByPartnership: Map<UUID, Int> = partnerships.associate { p ->
            val pack = p.pricingPack()
            val price = if (pack == null) {
                0
            } else {
                val effectiveBase = p.packPriceOverride ?: pack.basePrice
                val requiredIds = requiredOptionIdsByPack[pack.id.value] ?: emptySet()
                val partnershipOptions = optionsByPartnership[p.id.value] ?: emptyList()
                val optionalSum = partnershipOptions
                    .filter { it.pack.id.value == pack.id.value }
                    .filter { it.option.id.value !in requiredIds }
                    .sumOf { it.effectivePrice() }
                effectiveBase + optionalSum
            }
            p.id.value to price
        }

        val paid = partnerships
            .filter { it.id.value in paidPartnershipIds }
            .sumOf { priceByPartnership[it.id.value] ?: 0 }
        val validated = partnerships
            .filter { it.validatedAt != null }
            .sumOf { priceByPartnership[it.id.value] ?: 0 }
        val total = partnerships.sumOf { priceByPartnership[it.id.value] ?: 0 }

        EventBudget(
            currency = "EUR",
            totals = BudgetTotals(
                paid = paid,
                validated = validated,
                validatedMinusPaid = validated - paid,
                total = total,
                totalMinusValidated = total - validated,
            ),
            packs = emptyList(), // Filled in Task 5
        )
    }
}
```

- [ ] **Step 4: Run the test**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest"`
Expected: both tests PASS.

- [ ] **Step 5: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetRepositoryExposed.kt application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt
git commit -m "feat(server): compute EventBudget totals (paid / validated / total + diffs)"
```

---

## Task 5: Per-validated-pack breakdown

**Files:**
- Modify: `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetRepositoryExposed.kt`

- [ ] **Step 1: Add the failing test**

Append to `EventBudgetRoutesTest.kt`:

```kotlin
    @Test
    @Suppress("LongMethod")
    fun `groups validated partnerships by pack with company name and price applied`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()
        val goldPackId = UUID.randomUUID()
        val silverPackId = UUID.randomUUID()

        val acmeCompanyId = UUID.randomUUID()
        val betaCompanyId = UUID.randomUUID()
        val gammaCompanyId = UUID.randomUUID()
        val deltaCompanyId = UUID.randomUUID()

        val acmePartnershipId = UUID.randomUUID()
        val betaPartnershipId = UUID.randomUUID()
        val gammaPartnershipId = UUID.randomUUID()
        val deltaPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)

                fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack(
                    id = goldPackId, eventId = eventId, name = "Gold", basePrice = 1000,
                )
                fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack(
                    id = silverPackId, eventId = eventId, name = "Silver", basePrice = 500,
                )

                fr.devlille.partners.connect.companies.factories.insertMockedCompany(acmeCompanyId, name = "Acme")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(betaCompanyId, name = "Beta")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(gammaCompanyId, name = "Gamma")
                fr.devlille.partners.connect.companies.factories.insertMockedCompany(deltaCompanyId, name = "Delta")

                // Gold: Acme (override 1500) and Beta (catalogue 1000)
                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = acmePartnershipId, eventId = eventId, companyId = acmeCompanyId, selectedPackId = goldPackId,
                ).also { it.packPriceOverride = 1500 }
                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = betaPartnershipId, eventId = eventId, companyId = betaCompanyId, selectedPackId = goldPackId,
                )

                // Silver: Gamma
                fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership(
                    id = gammaPartnershipId, eventId = eventId, companyId = gammaCompanyId, selectedPackId = silverPackId,
                )

                // Non-validated with selectedPack — must NOT appear in packs[]
                fr.devlille.partners.connect.partnership.factories.insertMockedPartnership(
                    id = deltaPartnershipId, eventId = eventId, companyId = deltaCompanyId, selectedPackId = goldPackId,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val packs = body["packs"]!!.jsonArray
        assertEquals(2, packs.size)
        // Sorted by pack_name asc
        assertEquals("Gold", packs[0].jsonObject["pack_name"]!!.jsonPrimitive.content)
        assertEquals("Silver", packs[1].jsonObject["pack_name"]!!.jsonPrimitive.content)

        val gold = packs[0].jsonObject
        assertEquals(1000, gold["base_price"]!!.jsonPrimitive.content.toInt())
        val goldPartnerships = gold["partnerships"]!!.jsonArray
        assertEquals(2, goldPartnerships.size)
        // Within a pack, sorted by company_name asc
        assertEquals("Acme", goldPartnerships[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1500, goldPartnerships[0].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
        assertEquals("Beta", goldPartnerships[1].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1000, goldPartnerships[1].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())

        val silver = packs[1].jsonObject
        val silverPartnerships = silver["partnerships"]!!.jsonArray
        assertEquals(1, silverPartnerships.size)
        assertEquals("Gamma", silverPartnerships[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(500, silverPartnerships[0].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest.groups validated partnerships by pack with company name and price applied"`
Expected: FAIL — `packs` is empty.

- [ ] **Step 3: Implement the per-pack grouping**

In `EventBudgetRepositoryExposed.kt`, replace the final `EventBudget(...)` construction. Just before it, add the grouping logic, then update the `packs = ...` field:

```kotlin
        val packBudgets = partnerships
            .mapNotNull { p ->
                val validatedPack = p.validatedPack() ?: return@mapNotNull null
                Triple(validatedPack, p, priceByPartnership[p.id.value] ?: 0)
            }
            .groupBy { (pack, _, _) -> pack.id.value }
            .map { (_, triples) ->
                val pack = triples.first().first
                PackBudget(
                    packId = pack.id.value.toString(),
                    packName = pack.name,
                    basePrice = pack.basePrice,
                    partnerships = triples
                        .sortedBy { (_, partnership, _) -> partnership.company.name.lowercase() }
                        .map { (_, partnership, price) ->
                            fr.devlille.partners.connect.events.domain.PartnershipBudgetItem(
                                partnershipId = partnership.id.value.toString(),
                                companyName = partnership.company.name,
                                priceApplied = price,
                            )
                        },
                )
            }
            .sortedBy { it.packName.lowercase() }
```

Then change the final `packs = emptyList()` to `packs = packBudgets`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest"`
Expected: all three tests PASS.

- [ ] **Step 5: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventBudgetRepositoryExposed.kt application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt
git commit -m "feat(server): per-validated-pack breakdown in EventBudget response"
```

---

## Task 6: 404 for unknown event

**Files:**
- Modify: `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt`

- [ ] **Step 1: Add the failing test**

Append to `EventBudgetRoutesTest.kt`:

```kotlin
    @Test
    fun `returns 404 when event slug does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/events/does-not-exist/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
```

- [ ] **Step 2: Run the test**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest.returns 404 when event slug does not exist"`
Expected: PASS (the `EventEntity.findBySlug(...) ?: throw NotFoundException(...)` line already handles it). If it fails for any reason, fix the route or repository so it correctly throws `NotFoundException` and let StatusPages map it to 404.

- [ ] **Step 3: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt
git commit -m "test(server): cover 404 when event slug does not exist for budget"
```

---

## Task 7: Authorisation tests

**Files:**
- Modify: `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt`

- [ ] **Step 1: Add the failing tests**

Append:

```kotlin
    @Test
    fun `returns 401 when no auth header is provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `returns 403 when user is not member of the owning organisation`() = testApplication {
        val ownerUserId = UUID.randomUUID()
        val outsiderUserId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        application {
            // moduleSharedDb authenticates as `outsiderUserId` (mock engine)
            moduleSharedDb(userId = outsiderUserId)
            transaction {
                insertMockedUser(ownerUserId)
                insertMockedUser(outsiderUserId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = ownerUserId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
```

- [ ] **Step 2: Run the tests**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetRoutesTest"`
Expected: all PASS. The auth behaviour is provided by `AuthorizedOrganisationPlugin` and `moduleSharedDb`, identical to existing routes — no implementation change should be needed. If 403 case fails (returns 200 instead), the `AuthorizedOrganisationPlugin` install in `EventBudgetRoutes.kt` is missing or in the wrong scope; verify the plugin is installed inside the `route(...)` block.

- [ ] **Step 3: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetRoutesTest.kt
git commit -m "test(server): cover 401/403 auth for /budget endpoint"
```

---

## Task 8: End-to-end integration test (lifecycle)

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetLifecycleRoutesTest.kt`

This test exercises the realistic flow: create partnerships, validate some, bill+mark one as paid, decline another, then assert the full payload — including that `price_applied` correctly reflects an optional option with a `priceOverride`. It catches integration bugs between billing, partnership-options, and the budget aggregation.

- [ ] **Step 1: Create the test file**

```kotlin
package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.factories.insertMockedBilling
import fr.devlille.partners.connect.partnership.factories.insertMockedOptionPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.factories.insertMockedAttachPackOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBudgetLifecycleRoutesTest {
    @Test
    @Suppress("LongMethod")
    fun `full lifecycle - paid validated unvalidated declined with option overrides`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()
        val goldPackId = UUID.randomUUID()
        val optionId = UUID.randomUUID()

        val paidCompanyId = UUID.randomUUID()
        val validatedCompanyId = UUID.randomUUID()
        val pendingCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()

        val paidPartnershipId = UUID.randomUUID()
        val validatedPartnershipId = UUID.randomUUID()
        val pendingPartnershipId = UUID.randomUUID()
        val declinedPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)

                insertMockedSponsoringPack(id = goldPackId, eventId = eventId, name = "Gold", basePrice = 1000)
                // Optional TEXT option priced at 200, attached to Gold as NOT required.
                insertMockedSponsoringOption(
                    optionId = optionId, eventId = eventId, optionType = OptionType.TEXT, price = 200,
                )
                insertMockedAttachPackOption(packId = goldPackId, optionId = optionId, required = false)

                insertMockedCompany(paidCompanyId, name = "Acme")
                insertMockedCompany(validatedCompanyId, name = "Beta")
                insertMockedCompany(pendingCompanyId, name = "Gamma")
                insertMockedCompany(declinedCompanyId, name = "Delta")

                // Paid + validated + option with priceOverride 50 (instead of catalogue 200)
                insertMockedValidatedPartnership(
                    id = paidPartnershipId, eventId = eventId, companyId = paidCompanyId, selectedPackId = goldPackId,
                )
                insertMockedOptionPartnership(
                    partnershipId = paidPartnershipId,
                    packId = goldPackId,
                    optionId = optionId,
                    priceOverride = 50,
                )
                insertMockedBilling(
                    eventId = eventId,
                    partnershipId = paidPartnershipId,
                    status = InvoiceStatus.PAID,
                )

                // Validated only, no option, no billing
                insertMockedValidatedPartnership(
                    id = validatedPartnershipId, eventId = eventId, companyId = validatedCompanyId, selectedPackId = goldPackId,
                )

                // Pending — has a selectedPack so contributes to totals.total
                insertMockedPartnership(
                    id = pendingPartnershipId, eventId = eventId, companyId = pendingCompanyId, selectedPackId = goldPackId,
                )

                // Declined — excluded everywhere
                insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = goldPackId,
                    declinedAt = LocalDateTime.parse("2024-01-01T00:00:00"),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/budget") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // priceApplied(Acme) = base 1000 + optional option override 50 = 1050
        // priceApplied(Beta)  = base 1000
        // priceApplied(Gamma) = base 1000 (from selectedPack fallback)
        // priceApplied(Delta) = excluded (declined)
        val totals = body["totals"]!!.jsonObject
        assertEquals(1050, totals["paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(2050, totals["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["validated_minus_paid"]!!.jsonPrimitive.content.toInt())
        assertEquals(3050, totals["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1000, totals["total_minus_validated"]!!.jsonPrimitive.content.toInt())

        // Only validated partnerships appear in packs[]
        val packs = body["packs"]!!.jsonArray
        assertEquals(1, packs.size)
        val gold = packs[0].jsonObject
        assertEquals("Gold", gold["pack_name"]!!.jsonPrimitive.content)
        val partnerships = gold["partnerships"]!!.jsonArray
        assertEquals(2, partnerships.size)
        assertEquals("Acme", partnerships[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1050, partnerships[0].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
        assertEquals("Beta", partnerships[1].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals(1000, partnerships[1].jsonObject["price_applied"]!!.jsonPrimitive.content.toInt())
    }
}
```

- [ ] **Step 2: Verify or add the `insertMockedAttachPackOption` factory**

Check whether the factory exists by searching:
Run: `grep -rn "insertMockedAttachPackOption" /Users/gpaligot/Documents/workspace/partners-connect-1/server/application/src/test/kotlin/`

If it exists, skip to Step 3.

If it does NOT exist, add it to `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/factories/PackOptionsTable.factory.kt` (or create that file if missing — read `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/factories/PackOptionsTable.factory.kt` first to see the conventions). The function:

```kotlin
package fr.devlille.partners.connect.sponsoring.factories

import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID

fun insertMockedAttachPackOption(
    packId: UUID,
    optionId: UUID,
    required: Boolean = false,
) {
    PackOptionsTable.insert {
        it[pack] = EntityID(packId, fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable)
        it[option] = EntityID(optionId, fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable)
        it[PackOptionsTable.required] = required
    }
}
```

(If the factory file already exists, append the function without re-declaring the package.)

- [ ] **Step 3: Run the integration test**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test --tests "fr.devlille.partners.connect.events.EventBudgetLifecycleRoutesTest"`
Expected: PASS.

If it fails on the `priceApplied` for Acme (expected 1050, got 1000 or 1200): the issue is that the pricing helper isn't filtering by pack-required correctly OR isn't applying `priceOverride`. Re-read `EventBudgetPricing.kt` against `PartnershipOptionEntity.toDomain` in `partnership/application/mappers/PartnershipOptionEntity.ext.kt:43-127` and align.

- [ ] **Step 4: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/EventBudgetLifecycleRoutesTest.kt
# Also add the factory file if it was created/modified:
git add application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/factories/PackOptionsTable.factory.kt
git commit -m "test(server): add EventBudget end-to-end lifecycle integration test"
```

---

## Task 9: OpenAPI schemas and path

**Files:**
- Create: `application/src/main/resources/schemas/event_budget.schema.json`
- Create: `application/src/main/resources/schemas/budget_totals.schema.json`
- Create: `application/src/main/resources/schemas/pack_budget.schema.json`
- Create: `application/src/main/resources/schemas/partnership_budget_item.schema.json`
- Modify: `application/src/main/resources/openapi/openapi.yaml`

- [ ] **Step 1: Create `event_budget.schema.json`**

```json
{
  "$id": "event_budget.schema.json",
  "type": "object",
  "properties": {
    "currency": { "type": "string" },
    "totals": { "$ref": "budget_totals.schema.json" },
    "packs": {
      "type": "array",
      "items": { "$ref": "pack_budget.schema.json" }
    }
  },
  "required": ["currency", "totals", "packs"],
  "additionalProperties": false
}
```

- [ ] **Step 2: Create `budget_totals.schema.json`**

```json
{
  "$id": "budget_totals.schema.json",
  "type": "object",
  "properties": {
    "paid": { "type": "integer" },
    "validated": { "type": "integer" },
    "validated_minus_paid": { "type": "integer" },
    "total": { "type": "integer" },
    "total_minus_validated": { "type": "integer" }
  },
  "required": ["paid", "validated", "validated_minus_paid", "total", "total_minus_validated"],
  "additionalProperties": false
}
```

- [ ] **Step 3: Create `pack_budget.schema.json`**

```json
{
  "$id": "pack_budget.schema.json",
  "type": "object",
  "properties": {
    "pack_id": { "type": "string", "format": "uuid" },
    "pack_name": { "type": "string" },
    "base_price": { "type": "integer", "minimum": 0 },
    "partnerships": {
      "type": "array",
      "items": { "$ref": "partnership_budget_item.schema.json" }
    }
  },
  "required": ["pack_id", "pack_name", "base_price", "partnerships"],
  "additionalProperties": false
}
```

- [ ] **Step 4: Create `partnership_budget_item.schema.json`**

```json
{
  "$id": "partnership_budget_item.schema.json",
  "type": "object",
  "properties": {
    "partnership_id": { "type": "string", "format": "uuid" },
    "company_name": { "type": "string" },
    "price_applied": { "type": "integer", "minimum": 0 }
  },
  "required": ["partnership_id", "company_name", "price_applied"],
  "additionalProperties": false
}
```

- [ ] **Step 5: Register the four schemas in `openapi.yaml` components**

Edit `application/src/main/resources/openapi/openapi.yaml`. Find the line `    QandaStats:` (around line 6420) and add the four new entries right after it (alphabetical position within `components.schemas`):

```yaml
    EventBudget:
      $ref: "../schemas/event_budget.schema.json"
    BudgetTotals:
      $ref: "../schemas/budget_totals.schema.json"
    PackBudget:
      $ref: "../schemas/pack_budget.schema.json"
    PartnershipBudgetItem:
      $ref: "../schemas/partnership_budget_item.schema.json"
```

- [ ] **Step 6: Register the new path in `openapi.yaml`**

In `application/src/main/resources/openapi/openapi.yaml`, locate the existing `/orgs/{orgSlug}/events/{eventSlug}/stats:` block (around line 2472). Right after its block ends (the `"404"` ErrorResponse close, before the next `/orgs/{orgSlug}/events/{eventSlug}/booth-plan:`), insert:

```yaml
  /orgs/{orgSlug}/events/{eventSlug}/budget:
    get:
      summary: "Get event budget"
      operationId: "getOrgsEventsBudget"
      description: "Returns budget totals (paid, validated, total + diffs) and per-validated-pack breakdown listing partnerships with company name and price applied. Excludes declined partnerships."
      security:
        - bearerAuth: []
      parameters:
        - name: "orgSlug"
          in: "path"
          required: true
          schema:
            type: "string"
        - name: "eventSlug"
          in: "path"
          required: true
          schema:
            type: "string"
      responses:
        "200":
          description: "OK"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/EventBudget"
        "401":
          description: "Unauthorized"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        "403":
          description: "Forbidden"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        "404":
          description: "Event not found"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
```

- [ ] **Step 7: Validate and bundle the OpenAPI spec**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && npm run validate`
Expected: no errors.

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && npm run bundle`
Expected: writes a refreshed `application/src/main/resources/openapi/documentation.yaml` with the new path/schemas inlined.

- [ ] **Step 8: Commit**

```bash
git add application/src/main/resources/schemas/event_budget.schema.json application/src/main/resources/schemas/budget_totals.schema.json application/src/main/resources/schemas/pack_budget.schema.json application/src/main/resources/schemas/partnership_budget_item.schema.json application/src/main/resources/openapi/openapi.yaml application/src/main/resources/openapi/documentation.yaml
git commit -m "docs(server): document GET /orgs/{org}/events/{event}/budget in OpenAPI"
```

---

## Task 10: Full test + lint pass

- [ ] **Step 1: Run the full test suite**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:test`
Expected: BUILD SUCCESSFUL. All tests pass, including the new `EventBudgetRoutesTest` and `EventBudgetLifecycleRoutesTest`.

- [ ] **Step 2: Run detekt / ktlint**

Run: `cd /Users/gpaligot/Documents/workspace/partners-connect-1/server && ./gradlew :application:check`
Expected: BUILD SUCCESSFUL. If detekt complains about `LongMethod` on the long test methods, the `@Suppress("LongMethod")` annotations included in the test code should already silence it; if the lint flags new code outside tests, fix root cause rather than suppressing.

- [ ] **Step 3: No commit needed unless lint required code changes.** If lint required code changes:

```bash
git add -A
git commit -m "chore(server): satisfy detekt/ktlint after EventBudget addition"
```

---

## Self-Review (run before declaring complete)

**Spec coverage:**

| Spec requirement | Implemented in |
| --- | --- |
| FR-001 route & response shape | Task 3 (route), Task 1 (DTOs) |
| FR-002 priceApplied formula | Task 2 (`effectivePrice`), Task 4 (base+optional) |
| FR-003 pricingPack fallback | Task 2 (`pricingPack()`) |
| FR-004 paid total | Task 4 |
| FR-005 validated total | Task 4 |
| FR-006 total over non-declined w/ pack | Task 4 |
| FR-007 diffs computed server-side | Task 4 |
| FR-008 packs[] only from validatedPack | Task 5 |
| FR-009 partnership item shape | Task 1, Task 5 |
| FR-010 sort by pack_name & company_name asc | Task 5 |
| FR-011 exclude declined | Task 4 (uses `declined = false` filter), test in Task 4 & 8 |
| FR-012 auth plugin | Task 3 (install AuthorizedOrganisationPlugin), test in Task 7 |
| FR-013 404 unknown event | Task 3 + Task 6 |
| FR-014 currency=EUR | Task 1, Task 3, Task 9 |
| FR-015 batched loads (no N+1) | Task 4 (one query each: billing, options, pack-option configs) |
| US1 totals view | Tasks 3-4 |
| US2 per-pack breakdown | Task 5 |
| US3 authorisation | Task 7 |
| Edge cases | Covered in Tasks 4, 5, 8 |

**Placeholder scan:** none — every code block is concrete; the only conditional step is Task 7 Step 2 ("if 403 returns 200, check plugin install"), which is debugging guidance, not a placeholder.

**Type consistency:** Reviewed — `EventBudget`, `BudgetTotals`, `PackBudget`, `PartnershipBudgetItem` field names match exactly between DTO file (Task 1), JSON Schema files (Task 9), and test assertions (Tasks 3-8). `pricingPack()` and `effectivePrice()` signatures match between Task 2 and Task 4.

---

**Plan complete and saved to `server/specs/025-event-budget/plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
