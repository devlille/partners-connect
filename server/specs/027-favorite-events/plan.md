# Implementation Plan: 027-favorite-events

This plan is the technical companion to [`spec.md`](./spec.md). Read the spec first.

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three endpoints (`GET`, `PUT`, `DELETE`) under `/users/me/favorite-events` so an authenticated organiser can star, list, and un-star events that belong to organisations they have permission on.

**Architecture:** New `favorite_events` table joins `users` ↔ `events` with a unique pair. Clean-architecture layering: `domain/` interface + Exposed `application/` repository + Ktor `infrastructure/api/` route. Mounted on the existing `/users/me/...` pattern (bearer-token email resolution, no `AuthorizedOrganisationPlugin`). Repository enforces per-event org permission via the existing `OrganisationPermissionEntity.hasPermission` helper.

**Tech Stack:** Kotlin, Ktor, Exposed v1 ORM, Koin DI, kotlinx-serialization, kotlinx-datetime. Tests: kotlin.test + Ktor `testApplication` + H2 in-memory DB via `moduleSharedDb`.

---

## 1. Architecture summary

```
┌──────────────────────────────────────────────────────────────┐
│ partners-connect/server  (Ktor app, this workspace)          │
│                                                              │
│ App.kt                                                       │
│  └── routing { … favoriteEventRoutes() … }   ← new mount     │
│                                                              │
│ fr.devlille.partners.connect.events/                         │
│  ├── domain/                                                 │
│  │   └── FavoriteEventRepository.kt          ← new           │
│  ├── application/                                            │
│  │   ├── FavoriteEventRepositoryExposed.kt   ← new           │
│  │   └── mappers/                                            │
│  │       └── Event.ext.kt                    ← new (mapper)  │
│  └── infrastructure/                                         │
│      ├── api/                                                │
│      │   └── FavoriteEventRoutes.kt          ← new           │
│      ├── db/                                                 │
│      │   ├── FavoriteEventsTable.kt          ← new           │
│      │   └── FavoriteEventEntity.kt          ← new           │
│      └── bindings/                                           │
│          └── EventModule.kt                  ← edited        │
│                                                              │
│ internal/infrastructure/migrations/versions/                 │
│  └── AddFavoriteEventsTableMigration.kt      ← new           │
│ internal/infrastructure/migrations/                          │
│  └── MigrationRegistry.kt                    ← edited        │
│                                                              │
│ resources/openapi/openapi.yaml               ← edited        │
│ resources/openapi/documentation.yaml         ← regenerated   │
└──────────────────────────────────────────────────────────────┘
```

No new Koin module. No new JSON schemas. No new exception classes. `App.kt`'s existing `configureStatusPage()` already maps `UnauthorizedException → 401`, `ForbiddenException → 403`, `NotFoundException → 404`, `ConflictException → 409`.

---

## 2. File-by-file responsibilities

| File | Responsibility |
|---|---|
| `events/infrastructure/db/FavoriteEventsTable.kt` | Schema: `(user_id, event_id, favorited_at)` + `uniqueIndex(userId, eventId)` |
| `events/infrastructure/db/FavoriteEventEntity.kt` | DAO mapping + companion-object query helpers: `singleFavorite(userId, eventId)`, `listByUserOrderByEventStartTime(userId)` |
| `events/application/mappers/Event.ext.kt` | `fun EventEntity.toEventSummary(): EventSummary` — extracted from the inline mapping currently in `EventRepositoryExposed.findByUserEmail` |
| `events/domain/FavoriteEventRepository.kt` | Interface: `listByUserEmail`, `addFavorite`, `removeFavorite` (boolean returns for existence-collision paths) |
| `events/application/FavoriteEventRepositoryExposed.kt` | Exposed impl. Each method runs inside a single `transaction { }`. Uses existing helpers `UserEntity.singleUserByEmail`, `EventEntity.findBySlug`, `OrganisationPermissionEntity.hasPermission`. |
| `events/infrastructure/api/FavoriteEventRoutes.kt` | `fun Route.favoriteEventRoutes()` mounting `/users/me/favorite-events`. Resolves email from bearer token, delegates to repo, maps booleans → HTTP status codes (`true` → 201/204; `false` → 409/404). |
| `events/infrastructure/bindings/EventModule.kt` | Add `single<FavoriteEventRepository> { FavoriteEventRepositoryExposed() }` |
| `internal/infrastructure/migrations/versions/AddFavoriteEventsTableMigration.kt` | `SchemaUtils.createMissingTablesAndColumns(FavoriteEventsTable)` |
| `internal/infrastructure/migrations/MigrationRegistry.kt` | Append `AddFavoriteEventsTableMigration` to `allMigrations` |
| `App.kt` | Add `import …favoriteEventRoutes` + call `favoriteEventRoutes()` in `routing { }` right after `userRoutes()` |
| `events/application/EventRepositoryExposed.kt` | Replace the inline `EventEntity → EventSummary` literal (lines 249–256) with `event.toEventSummary()` |
| `resources/openapi/openapi.yaml` | Add three paths under the existing `/users/me/...` block, inline status codes referencing `#/components/schemas/EventSummary` and `#/components/schemas/ErrorResponse` |
| **Tests** | |
| `test/events/factories/FavoriteEventEntity.factory.kt` | `insertMockedFavoriteEvent(userId, eventId)` |
| `test/events/infrastructure/api/FavoriteEventRoutePutTest.kt` | PUT contract tests: 201, 401, 403, 404, 409 |
| `test/events/infrastructure/api/FavoriteEventRouteGetTest.kt` | GET contract tests: 200 empty, 200 with two favorites ordered by start_time, 401 |
| `test/events/infrastructure/api/FavoriteEventRouteDeleteTest.kt` | DELETE contract tests: 204, 401, 403, 404 |
| `test/events/FavoriteEventCrossOrgRoutesTest.kt` | One integration test exercising the full add → list → cross-org-403 → delete workflow across two users in two orgs |

---

## 3. Helper references (existing code the plan calls)

These already exist; the plan only **uses** them. Listed here so reviewers and implementers don't have to grep.

- `UserEntity.singleUserByEmail(email: String): UserEntity?` — `users/infrastructure/db/UserEntity.kt:17`
- `EventEntity.findBySlug(slug: String): EventEntity?` — `events/infrastructure/db/EventEntity.kt:29`
- `OrganisationPermissionEntity.hasPermission(orgId: UUID, userId: UUID): Boolean` — `users/infrastructure/db/OrganisationPermissionEntity.kt:36`
- `EventSummary` data class — `events/domain/EventSummary.kt`
- `call.token` (extension on `ApplicationCall`) — `internal/infrastructure/api/`
- `call.parameters.eventSlug` (extension on `StringValues`) — `events/infrastructure/api/StringValues.ext.kt:8`
- `AuthRepository.getUserInfo(token): UserInfo` — `auth/domain/AuthRepository.kt`
- `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, `ConflictException` — `internal/infrastructure/api/*Exception.kt`
- Test factories: `insertMockedUser(userId)`, `insertMockedOrganisationEntity(orgId)`, `insertMockedOrgaPermission(orgId, userId)`, `insertMockedFutureEventWithSlug(slug = ..., orgId = ...)` — see `test/.../factories/`
- Test harness: `moduleSharedDb(userId)` — see `test/internal/ApplicationMock.kt:46`. Sends `Authorization: Bearer valid` and the mock OAuth engine resolves to `UserEntity[userId]`.

---

## 4. Tasks

Each task is self-contained: it lists the files it touches, shows complete code (no placeholders), and ends with a commit. Tests are written before the code they exercise where practical, but Task 1 (pure scaffolding) and Task 2 (refactor) come first so the type signatures exist when the first test compiles.

---

### Task 1: Database scaffold — table, entity, migration

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/FavoriteEventsTable.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/FavoriteEventEntity.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/AddFavoriteEventsTableMigration.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/MigrationRegistry.kt`

- [ ] **Step 1: Create the table file**

```kotlin
// FavoriteEventsTable.kt
package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.time.Clock

object FavoriteEventsTable : UUIDTable("favorite_events") {
    val userId = reference("user_id", UsersTable)
    val eventId = reference("event_id", EventsTable)
    val favoritedAt = datetime("favorited_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        uniqueIndex(userId, eventId)
    }
}
```

- [ ] **Step 2: Create the entity file with query helpers**

```kotlin
// FavoriteEventEntity.kt
package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.UUID

class FavoriteEventEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FavoriteEventEntity>(FavoriteEventsTable)

    var user by UserEntity referencedOn FavoriteEventsTable.userId
    var event by EventEntity referencedOn FavoriteEventsTable.eventId
    var favoritedAt by FavoriteEventsTable.favoritedAt
}

fun UUIDEntityClass<FavoriteEventEntity>.singleFavorite(
    userId: UUID,
    eventId: UUID,
): FavoriteEventEntity? = this.find {
    (FavoriteEventsTable.userId eq userId) and (FavoriteEventsTable.eventId eq eventId)
}.singleOrNull()

fun UUIDEntityClass<FavoriteEventEntity>.listByUserOrderByEventStartTime(
    userId: UUID,
): List<FavoriteEventEntity> = this
    .find { FavoriteEventsTable.userId eq userId }
    .sortedBy { it.event.startTime }
```

- [ ] **Step 3: Create the migration**

```kotlin
// AddFavoriteEventsTableMigration.kt
package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddFavoriteEventsTableMigration : Migration {
    override val id = "20260524_add_favorite_events"
    override val description = "Add favorite_events join table linking users to events they have starred"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(FavoriteEventsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
```

- [ ] **Step 4: Register the migration in `MigrationRegistry.kt`**

Add the import alphabetically with the other `versions.*` imports, then append the entry to `allMigrations` (last position is fine — `MigrationManager` sorts by id at apply time, so chronological order is enforced by the date prefix in the id):

```kotlin
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddFavoriteEventsTableMigration
```

```kotlin
val allMigrations: List<Migration> = listOf(
    // … existing entries unchanged …
    AddFlyerTemplateColumnsToSponsoringPacksMigration,
    AddFavoriteEventsTableMigration,
)
```

- [ ] **Step 5: Compile-only sanity check**

Run: `./gradlew :application:compileKotlin --no-daemon`
Expected: BUILD SUCCESSFUL — no symbol-resolution errors. (Nothing functional to test yet; the entity and table are unreferenced by route code at this point.)

- [ ] **Step 6: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/FavoriteEventsTable.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/db/FavoriteEventEntity.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/AddFavoriteEventsTableMigration.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/MigrationRegistry.kt
git commit -m "feat(server): add favorite_events table and migration"
```

---

### Task 2: Extract `EventEntity → EventSummary` mapper

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/application/mappers/Event.ext.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventRepositoryExposed.kt` (lines 249–256)

- [ ] **Step 1: Create the mapper file**

```kotlin
// Event.ext.kt
package fr.devlille.partners.connect.events.application.mappers

import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity

fun EventEntity.toEventSummary(): EventSummary = EventSummary(
    slug = slug,
    name = name,
    startTime = startTime,
    endTime = endTime,
    submissionStartTime = submissionStartTime,
    submissionEndTime = submissionEndTime,
)
```

- [ ] **Step 2: Replace the inline literal in `EventRepositoryExposed.findByUserEmail`**

In `EventRepositoryExposed.kt`, replace the block starting at line ~247:

```kotlin
            orgEvents.forEach { event ->
                events.add(
                    EventSummary(
                        slug = event.slug,
                        name = event.name,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        submissionStartTime = event.submissionStartTime,
                        submissionEndTime = event.submissionEndTime,
                    ),
                )
            }
```

with:

```kotlin
            orgEvents.forEach { event ->
                events.add(event.toEventSummary())
            }
```

Add the import at the top of the file:

```kotlin
import fr.devlille.partners.connect.events.application.mappers.toEventSummary
```

- [ ] **Step 3: Verify existing tests still pass**

Run: `./gradlew :application:test --tests "*ListUserEventsRoute*" --no-daemon`
Expected: BUILD SUCCESSFUL — the existing `/users/me/events` tests must keep passing because the mapper is a pure refactor.

- [ ] **Step 4: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/application/mappers/Event.ext.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/events/application/EventRepositoryExposed.kt
git commit -m "refactor(server): extract EventEntity#toEventSummary mapper"
```

---

### Task 3: Domain interface + empty Exposed repository

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/domain/FavoriteEventRepository.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/application/FavoriteEventRepositoryExposed.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/bindings/EventModule.kt`

- [ ] **Step 1: Create the domain interface**

```kotlin
// FavoriteEventRepository.kt
package fr.devlille.partners.connect.events.domain

interface FavoriteEventRepository {
    fun listByUserEmail(userEmail: String): List<EventSummary>

    /**
     * @return true if a new favorite row was inserted, false if it already existed.
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException if the event slug is unknown.
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException if the event belongs to an org the caller has no permission on.
     */
    fun addFavorite(userEmail: String, eventSlug: String): Boolean

    /**
     * @return true if a favorite row was deleted, false if no favorite existed (covers both unknown event and known-but-not-favorited).
     * @throws fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException if the event exists but belongs to an org the caller has no permission on.
     */
    fun removeFavorite(userEmail: String, eventSlug: String): Boolean
}
```

- [ ] **Step 2: Create the Exposed implementation**

```kotlin
// FavoriteEventRepositoryExposed.kt
package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.application.mappers.toEventSummary
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.domain.FavoriteEventRepository
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.events.infrastructure.db.listByUserOrderByEventStartTime
import fr.devlille.partners.connect.events.infrastructure.db.singleFavorite
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.hasPermission
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FavoriteEventRepositoryExposed : FavoriteEventRepository {
    override fun listByUserEmail(userEmail: String): List<EventSummary> = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException("User with email $userEmail not found")
        FavoriteEventEntity
            .listByUserOrderByEventStartTime(user.id.value)
            .filter { favorite ->
                OrganisationPermissionEntity.hasPermission(
                    organisationId = favorite.event.organisation.id.value,
                    userId = user.id.value,
                )
            }
            .map { it.event.toEventSummary() }
    }

    override fun addFavorite(userEmail: String, eventSlug: String): Boolean = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException("User with email $userEmail not found")
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event $eventSlug not found")
        if (!OrganisationPermissionEntity.hasPermission(event.organisation.id.value, user.id.value)) {
            throw ForbiddenException("You are not allowed to favorite this event")
        }
        if (FavoriteEventEntity.singleFavorite(user.id.value, event.id.value) != null) {
            return@transaction false
        }
        FavoriteEventEntity.new {
            this.user = user
            this.event = event
        }
        true
    }

    override fun removeFavorite(userEmail: String, eventSlug: String): Boolean = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException("User with email $userEmail not found")
        val event = EventEntity.findBySlug(eventSlug) ?: return@transaction false
        if (!OrganisationPermissionEntity.hasPermission(event.organisation.id.value, user.id.value)) {
            throw ForbiddenException("You are not allowed to manage favorites for this event")
        }
        val favorite = FavoriteEventEntity.singleFavorite(user.id.value, event.id.value)
            ?: return@transaction false
        favorite.delete()
        true
    }
}
```

Notes on the implementation:
- `listByUserEmail` filters by current org permission (FR-010): a favorite whose underlying event belongs to an org the user has been revoked from is silently hidden.
- `addFavorite` throws `NotFoundException` for unknown event (FR-006); `removeFavorite` returns `false` for unknown event so the route surfaces the uniform "not in your favorites" 404 (FR-009).
- Both writes check permission *before* the favorite-existence check, so cross-org attempts always return 403 even when there is no favorite row to consider.
- `import io.ktor.server.plugins.NotFoundException` matches the existing project usage (e.g. `EventRepositoryExposed.kt:227`); `ForbiddenException` is the internal one at `internal/infrastructure/api/ForbiddenException.kt`.

- [ ] **Step 3: Register the repository in `EventModule.kt`**

Add the imports and the `single<FavoriteEventRepository>` block:

```kotlin
import fr.devlille.partners.connect.events.application.FavoriteEventRepositoryExposed
import fr.devlille.partners.connect.events.domain.FavoriteEventRepository
```

```kotlin
val eventModule = module {
    single<EventRepository> { EventRepositoryExposed(EventEntity) }
    single<EventStorageRepository> { EventStorageRepositoryGoogleStorage(get()) }
    single<EventStatsRepository> { EventStatsRepositoryExposed() }
    single<EventBudgetRepository> { EventBudgetRepositoryExposed() }
    single<FavoriteEventRepository> { FavoriteEventRepositoryExposed() }
}
```

- [ ] **Step 4: Compile-only sanity check**

Run: `./gradlew :application:compileKotlin --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/domain/FavoriteEventRepository.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/events/application/FavoriteEventRepositoryExposed.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/bindings/EventModule.kt
git commit -m "feat(server): add FavoriteEventRepository (domain + Exposed impl)"
```

---

### Task 4: Ktor route + mount in `App.kt`

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRoutes.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/App.kt`

- [ ] **Step 1: Create the route file**

```kotlin
// FavoriteEventRoutes.kt
package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.FavoriteEventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.favoriteEventRoutes() {
    val authRepository by inject<AuthRepository>()
    val favoriteRepository by inject<FavoriteEventRepository>()

    route("/users/me/favorite-events") {
        get {
            val userInfo = authRepository.getUserInfo(call.token)
            call.respond(HttpStatusCode.OK, favoriteRepository.listByUserEmail(userInfo.email))
        }
        put("/{eventSlug}") {
            val userInfo = authRepository.getUserInfo(call.token)
            val eventSlug = call.parameters.eventSlug
            val added = favoriteRepository.addFavorite(userInfo.email, eventSlug)
            if (!added) {
                throw ConflictException("Event $eventSlug is already in your favorites")
            }
            call.respond(HttpStatusCode.Created)
        }
        delete("/{eventSlug}") {
            val userInfo = authRepository.getUserInfo(call.token)
            val eventSlug = call.parameters.eventSlug
            val removed = favoriteRepository.removeFavorite(userInfo.email, eventSlug)
            if (!removed) {
                throw NotFoundException("Event $eventSlug is not in your favorites")
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
```

`call.parameters.eventSlug` is the extension at `events/infrastructure/api/StringValues.ext.kt:8` — already in this package.

- [ ] **Step 2: Mount in `App.kt`**

Add the import (alphabetically near the other `events.infrastructure.api.*` imports):

```kotlin
import fr.devlille.partners.connect.events.infrastructure.api.favoriteEventRoutes
```

In the `routing { … }` block, add the call directly after `userRoutes()` so favorites sit with the other `/users/me/...` routes (find `userRoutes()` at `App.kt:150`):

```kotlin
        userRoutes()
        favoriteEventRoutes()
```

- [ ] **Step 3: Build to confirm the route compiles and mounts**

Run: `./gradlew :application:compileKotlin --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRoutes.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/App.kt
git commit -m "feat(server): add /users/me/favorite-events GET/PUT/DELETE routes"
```

---

### Task 5: Test factory for favorites

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/events/factories/FavoriteEventEntity.factory.kt`

- [ ] **Step 1: Create the factory**

```kotlin
// FavoriteEventEntity.factory.kt
package fr.devlille.partners.connect.events.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import java.util.UUID

fun insertMockedFavoriteEvent(
    userId: UUID,
    eventId: UUID,
): FavoriteEventEntity = FavoriteEventEntity.new {
    this.user = UserEntity[userId]
    this.event = EventEntity[eventId]
}
```

The `favoritedAt` column defaults to `Clock.System.now()` via `clientDefault` on the table.

- [ ] **Step 2: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/factories/FavoriteEventEntity.factory.kt
git commit -m "test(server): add insertMockedFavoriteEvent factory"
```

---

### Task 6: PUT contract tests (TDD — write first, then verify)

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRoutePutTest.kt`

The route handler from Task 4 already implements all the PUT paths. These tests verify each documented status code.

- [ ] **Step 1: Write the test class with all five PUT cases**

```kotlin
package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFavoriteEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventRoutePutTest {
    @Test
    fun `PUT returns 201 on first add`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-a"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = orgId)
            }
        }

        val response = client.put("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `PUT returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        application { moduleSharedDb(userId) }

        val response = client.put("/users/me/favorite-events/anything")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 404 when event slug is unknown`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
        }

        val response = client.put("/users/me/favorite-events/does-not-exist") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 403 when event belongs to an org the caller has no permission on`() = testApplication {
        val userId = UUID.randomUUID()
        val ownerOrgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-other-org"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(ownerOrgId)
                // NO insertMockedOrgaPermission for userId on ownerOrgId
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = ownerOrgId)
            }
        }

        val response = client.put("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `PUT returns 409 when caller has already favorited this event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-already-favorited"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = orgId)
                insertMockedFavoriteEvent(userId = userId, eventId = eventId)
            }
        }

        val response = client.put("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :application:test --tests "*FavoriteEventRoutePutTest" --no-daemon`
Expected: BUILD SUCCESSFUL with 5 passing tests.

- [ ] **Step 3: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRoutePutTest.kt
git commit -m "test(server): contract tests for PUT /users/me/favorite-events/{eventSlug}"
```

---

### Task 7: GET contract tests

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRouteGetTest.kt`

- [ ] **Step 1: Write the test class with three GET cases**

```kotlin
package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFavoriteEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
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
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventRouteGetTest {
    @Test
    fun `GET returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        application { moduleSharedDb(userId) }

        val response = client.get("/users/me/favorite-events")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns 200 with empty array when caller has no favorites`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction { insertMockedUser(userId) }
        }

        val response = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns favorites in two orgs ordered by start_time ascending`() = testApplication {
        val userId = UUID.randomUUID()
        val orgAId = UUID.randomUUID()
        val orgBId = UUID.randomUUID()
        val laterEventId = UUID.randomUUID()
        val earlierEventId = UUID.randomUUID()
        val laterSlug = "event-later"
        val earlierSlug = "event-earlier"
        // Pick start_times explicitly so the ordering assertion is deterministic.
        // insertMockedFutureEventWithSlug uses default start_time = next-year Dec 1; override via the wrapper for the earlier event.
        val laterStart = LocalDateTime.parse("2030-12-01T00:00:00")
        val earlierStart = LocalDateTime.parse("2030-06-01T00:00:00")

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgAId)
                insertMockedOrganisationEntity(orgBId)
                insertMockedOrgaPermission(orgAId, userId = userId)
                insertMockedOrgaPermission(orgBId, userId = userId)
                insertMockedFutureEventWithSlug(id = laterEventId, slug = laterSlug, orgId = orgAId).apply {
                    startTime = laterStart
                }
                insertMockedFutureEventWithSlug(id = earlierEventId, slug = earlierSlug, orgId = orgBId).apply {
                    startTime = earlierStart
                }
                insertMockedFavoriteEvent(userId = userId, eventId = laterEventId)
                insertMockedFavoriteEvent(userId = userId, eventId = earlierEventId)
            }
        }

        val response = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, items.size)
        assertEquals(earlierSlug, items[0].jsonObject["slug"]?.jsonPrimitive?.content)
        assertEquals(laterSlug, items[1].jsonObject["slug"]?.jsonPrimitive?.content)
    }
}
```

Mutating `startTime` after `insertMockedFutureEventWithSlug` works because the factory returns the `EventEntity` and Exposed DAO writes are flushed at transaction commit. This avoids extending the factory just to express two test events with two different start times.

- [ ] **Step 2: Run the tests**

Run: `./gradlew :application:test --tests "*FavoriteEventRouteGetTest" --no-daemon`
Expected: BUILD SUCCESSFUL with 3 passing tests.

- [ ] **Step 3: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRouteGetTest.kt
git commit -m "test(server): contract tests for GET /users/me/favorite-events"
```

---

### Task 8: DELETE contract tests

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRouteDeleteTest.kt`

- [ ] **Step 1: Write the test class with four DELETE cases**

```kotlin
package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFavoriteEvent
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventRouteDeleteTest {
    @Test
    fun `DELETE returns 204 when favorite exists`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-to-delete"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = orgId)
                insertMockedFavoriteEvent(userId = userId, eventId = eventId)
            }
        }

        val response = client.delete("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        application { moduleSharedDb(userId) }

        val response = client.delete("/users/me/favorite-events/anything")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE returns 404 when event slug is unknown`() = testApplication {
        val userId = UUID.randomUUID()
        application {
            moduleSharedDb(userId)
            transaction { insertMockedUser(userId) }
        }

        val response = client.delete("/users/me/favorite-events/does-not-exist") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE returns 404 when event exists but is not in the caller's favorites`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-not-favorited"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = orgId)
                // NO insertMockedFavoriteEvent
            }
        }

        val response = client.delete("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE returns 403 when event belongs to an org the caller has no permission on`() = testApplication {
        val userId = UUID.randomUUID()
        val ownerOrgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val slug = "event-other-org"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(ownerOrgId)
                // NO insertMockedOrgaPermission for userId on ownerOrgId
                insertMockedFutureEventWithSlug(id = eventId, slug = slug, orgId = ownerOrgId)
            }
        }

        val response = client.delete("/users/me/favorite-events/$slug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew :application:test --tests "*FavoriteEventRouteDeleteTest" --no-daemon`
Expected: BUILD SUCCESSFUL with 5 passing tests.

- [ ] **Step 3: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/infrastructure/api/FavoriteEventRouteDeleteTest.kt
git commit -m "test(server): contract tests for DELETE /users/me/favorite-events/{eventSlug}"
```

---

### Task 9: Cross-org integration test

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/events/FavoriteEventCrossOrgRoutesTest.kt`

Per the `integration-tests` skill, integration tests live at the feature root (not under `infrastructure/api/`), use plural "RoutesTest" naming, and chain multiple HTTP calls in a single workflow.

The test below uses two users (A and B) in two orgs (A and B). The mock OAuth engine only resolves `"Bearer valid"` to the *one* `userId` passed to `moduleSharedDb`. To exercise user-B's perspective, restart `testApplication { }` is not needed — we instead make B's check a database-level assertion: after A favorites their event, query the table directly to assert no row exists for B. The cross-org 403 case is exercised via the HTTP route by having A try to favorite B's event.

- [ ] **Step 1: Write the integration test**

```kotlin
package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventEntity
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventsTable
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoriteEventCrossOrgRoutesTest {
    @Test
    @Suppress("LongMethod")
    fun `user A can favorite own org event, cannot favorite other org event, isolated from user B`() = testApplication {
        val userAId = UUID.randomUUID()
        val userBId = UUID.randomUUID()
        val orgAId = UUID.randomUUID()
        val orgBId = UUID.randomUUID()
        val eventAId = UUID.randomUUID()
        val eventBId = UUID.randomUUID()
        val eventASlug = "event-org-a"
        val eventBSlug = "event-org-b"

        // The mock OAuth engine resolves "Bearer valid" to userAId here.
        application {
            moduleSharedDb(userId = userAId)
            transaction {
                insertMockedUser(userAId)
                insertMockedUser(userBId)
                insertMockedOrganisationEntity(orgAId)
                insertMockedOrganisationEntity(orgBId)
                insertMockedOrgaPermission(orgAId, userId = userAId)
                insertMockedOrgaPermission(orgBId, userId = userBId)
                insertMockedFutureEventWithSlug(id = eventAId, slug = eventASlug, orgId = orgAId)
                insertMockedFutureEventWithSlug(id = eventBId, slug = eventBSlug, orgId = orgBId)
            }
        }

        // Step 1: user A favorites event-A → 201
        val addAResp = client.put("/users/me/favorite-events/$eventASlug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Created, addAResp.status)

        // Step 2: user A lists favorites → contains event-A only
        val listResp = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, listResp.status)
        val items = Json.parseToJsonElement(listResp.bodyAsText()).jsonArray
        assertEquals(1, items.size)
        assertEquals(eventASlug, items[0].jsonObject["slug"]?.jsonPrimitive?.content)

        // Step 3: user A tries to favorite event-B (org B, no permission) → 403
        val crossOrgResp = client.put("/users/me/favorite-events/$eventBSlug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Forbidden, crossOrgResp.status)

        // Step 4: user B's favorites are isolated from user A's — verified at the DB layer
        //         (the mock OAuth engine is fixed to userAId; we can't switch identity mid-test,
        //         so we assert state directly).
        transaction {
            val rowsForUserB = FavoriteEventEntity
                .find { FavoriteEventsTable.userId eq userBId }
                .count()
            assertEquals(0, rowsForUserB, "User B should have zero favorites")

            val rowsForUserA = FavoriteEventEntity
                .find { FavoriteEventsTable.userId eq userAId }
                .count()
            assertEquals(1, rowsForUserA, "User A should have exactly one favorite")
        }

        // Step 5: user A removes their favorite → 204
        val deleteResp = client.delete("/users/me/favorite-events/$eventASlug") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResp.status)

        // Step 6: user A lists favorites again → empty
        val finalListResp = client.get("/users/me/favorite-events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, finalListResp.status)
        assertEquals("[]", finalListResp.bodyAsText())
    }
}
```

- [ ] **Step 2: Run the integration test**

Run: `./gradlew :application:test --tests "*FavoriteEventCrossOrgRoutesTest" --no-daemon`
Expected: BUILD SUCCESSFUL with 1 passing test.

- [ ] **Step 3: Commit**

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/events/FavoriteEventCrossOrgRoutesTest.kt
git commit -m "test(server): integration test for favorite-events cross-org isolation"
```

---

### Task 10: OpenAPI documentation

**Files:**
- Modify: `application/src/main/resources/openapi/openapi.yaml`
- Modify: `application/src/main/resources/openapi/documentation.yaml` (regenerated via `npm run bundle`)

- [ ] **Step 1: Add three paths to `openapi.yaml`**

Locate the existing `/users/me/orgs:` block (around line 5937) and append the three new paths *immediately after* it, before the `components:` block (around line 5953). Use the inline-status-code style with `$ref: "#/components/schemas/ErrorResponse"` for failure bodies, mirroring `/orgs/{orgSlug}/ai/chat`.

```yaml
  /users/me/favorite-events:
    get:
      summary: "List the authenticated user's favorite events"
      operationId: "getUsersMeFavoriteEvents"
      description: "Returns the caller's favorite events ordered by start_time ascending. Favorites whose event belongs to an organisation the caller no longer has permission on are filtered out."
      security:
        - bearerAuth: []
      responses:
        "200":
          description: "OK"
          content:
            application/json:
              schema:
                type: "array"
                items:
                  $ref: "#/components/schemas/EventSummary"
        "401":
          description: "Unauthorized - Missing or invalid authentication token"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
  /users/me/favorite-events/{eventSlug}:
    parameters:
      - name: "eventSlug"
        in: "path"
        required: true
        schema:
          type: "string"
        description: "Event slug to favorite or un-favorite"
    put:
      summary: "Add an event to the authenticated user's favorites"
      operationId: "putUsersMeFavoriteEvent"
      description: "Idempotent at the HTTP level only insofar as repeating the call surfaces a 409 Conflict; otherwise creates one row in favorite_events."
      security:
        - bearerAuth: []
      responses:
        "201":
          description: "Created"
        "401":
          description: "Unauthorized - Missing or invalid authentication token"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        "403":
          description: "Forbidden - The event exists but belongs to an organisation the caller has no permission on"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        "404":
          description: "Not Found - Unknown event slug"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        "409":
          description: "Conflict - The caller has already favorited this event"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
    delete:
      summary: "Remove an event from the authenticated user's favorites"
      operationId: "deleteUsersMeFavoriteEvent"
      description: "Returns 204 on success. Returns 404 for both an unknown event slug and a known event that is not in the caller's favorites."
      security:
        - bearerAuth: []
      responses:
        "204":
          description: "No Content - Favorite removed"
        "401":
          description: "Unauthorized - Missing or invalid authentication token"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        "403":
          description: "Forbidden - The event exists but belongs to an organisation the caller has no permission on"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        "404":
          description: "Not Found - The event is not in the caller's favorites (or the slug is unknown)"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
```

- [ ] **Step 2: Validate the OpenAPI source**

Run: `npm run validate`
Expected: passes with no errors. (Run from `application/src/main/resources/openapi/` if that's where `package.json` lives; otherwise from the project root — see existing scripts.)

- [ ] **Step 3: Bundle the OpenAPI**

Run: `npm run bundle`
Expected: regenerates `documentation.yaml`. The diff should show three new path entries under `/users/me/favorite-events*`.

- [ ] **Step 4: Commit**

```bash
git add application/src/main/resources/openapi/openapi.yaml \
        application/src/main/resources/openapi/documentation.yaml
git commit -m "docs(server): document /users/me/favorite-events endpoints in OpenAPI"
```

---

### Task 11: Final verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full Gradle quality gate**

Run: `./gradlew ktlintCheck detekt test --no-daemon`
Expected: BUILD SUCCESSFUL with all tests passing.

- [ ] **Step 2: Re-run the OpenAPI tooling**

Run: `npm run validate && npm run bundle`
Expected: both commands pass, no diff in `documentation.yaml` (already committed in Task 10).

- [ ] **Step 3: Manually verify the migration applied to a fresh DB**

Start the server locally (`./gradlew :application:run` or via the project's standard `docker compose` command) and check the migrations log for the line:

```
Applying migration: 20260524_add_favorite_events - Add favorite_events join table linking users to events they have starred
```

Then connect to the DB and confirm:

```sql
SELECT migration_id FROM applied_migrations WHERE migration_id = '20260524_add_favorite_events';
\d favorite_events
```

Expected: the migration row exists; the `favorite_events` table has columns `id`, `user_id`, `event_id`, `favorited_at` plus the unique index on `(user_id, event_id)`.

No commit for this task — verification only.

---

## 5. Spec coverage cross-check

| Spec item | Implemented in |
|---|---|
| FR-001 (GET endpoint, EventSummary, ordered ascending, empty=`[]`) | Tasks 3 (repo), 4 (route), 7 (tests) |
| FR-002 (PUT 201 / 409) | Tasks 3, 4, 6 |
| FR-003 (DELETE 204) | Tasks 3, 4, 8 |
| FR-004 (no `AuthorizedOrganisationPlugin`, bearer-only) | Task 4 |
| FR-005 (missing/invalid token → 401) | Tasks 6, 7, 8 (the `Authorization missing` tests) |
| FR-006 (PUT 404 on unknown slug) | Task 3 (repo throw), Task 6 (test) |
| FR-007 (403 on cross-org via `hasPermission`) | Task 3 (repo throw), Tasks 6, 8 (tests) |
| FR-008 (409 on duplicate; existence-check via `singleFavorite`) | Task 3 (repo), Task 6 (test) |
| FR-009 (DELETE 404 collapsed) | Task 3 (repo returns false), Task 4 (route throws 404), Task 8 (test covers both branches) |
| FR-010 (live filter of revoked-org favorites in GET) | Task 3 (repo `.filter { hasPermission }`) |
| FR-011 (OpenAPI updated, no new JSON schemas) | Task 10 |
| FR-012 (route mounted in `App.kt`) | Task 4 |
| FR-013 (`FavoriteEventRepository` registered in `EventModule.kt`, no new Koin module) | Task 3 |
| FR-014 (`ktlintCheck detekt test` pass) | Task 11 |
| Key entity: `FavoriteEventsTable` | Task 1 |
| Key entity: `FavoriteEventEntity` + helpers | Task 1 |
| Key entity: `FavoriteEventRepository` | Task 3 |
| Key entity: `FavoriteEventRepositoryExposed` | Task 3 |
| Key entity: `Route.favoriteEventRoutes()` | Task 4 |
| Key entity: `EventEntity.toEventSummary()` extracted mapper | Task 2 |
| SC-001 (12 documented status-code paths exercised) | Tasks 6+7+8 (5+3+5 = 13 contract tests, covers all 12 rows from the spec table) |
| SC-002 (CI gates pass) | Task 11 |
| SC-003 (contract-test coverage) | Tasks 6, 7, 8 |
| SC-004 (cross-user/cross-org integration coverage) | Task 9 |
| SC-005 (post-deploy smoke test) | Out-of-band; covered by deploying the feature and running the documented happy path |

No spec requirement is unaccounted for. No task references a type, method, or factory that has not been defined earlier in this plan or in the existing codebase.
