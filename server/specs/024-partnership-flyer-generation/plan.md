# Partnership Flyer Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let organisers attach a PNG flyer template + logo zone to a sponsoring pack, then trigger backend-side composition of a per-partnership flyer that lands as the partnership's communication support, with notifications to partners and Slack to organisers, and digest visibility for what remains to generate.

**Architecture:** Five nullable columns on `sponsoring_packs` carry the template URL + zone (x, y, width, height). A new `FlyerGenerationService` downloads the template from GCS and the company logo over HTTP, composites with `imgscalr.Scalr.resize` (preserve aspect ratio, 20px margin, centred), exports JPG, and reuses `PartnershipStorageRepository.uploadCommunicationSupport`. The route is org-only, fires `NotificationVariables.FlyerGenerated` via the existing `NotificationPartnershipPlugin` for email + Slack, and the morning digest gains a `flyerItems` section listing validated partnerships whose pack is flyer-enabled but whose `communication_support_url` is still null.

**Tech Stack:** Kotlin 2.x / Ktor / Exposed v1 / Koin / Google Cloud Storage / Mailjet / Slack / imgscalr-lib 4.2 / JDK ImageIO. Tests use Ktor `testApplication` with `moduleSharedDb` and existing factories.

**Scope note:** This plan is backend-only. Frontend (template upload UI in the pack form, zone-picker overlay, per-partnership "Generate" button) is tracked separately. The backend API surface defined here is sufficient for that frontend work to land independently.

**Out of scope:** Flyer review/approval state, batch generation, template versioning, locking of concurrent regenerations, SVG templates. See spec §"What I'd intentionally skip".

---

## File Structure

**Modified files:**

- `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringPacksTable.kt` — five new nullable columns.
- `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringPackEntity.kt` — five new entity properties + `hasFlyerTemplate()` helper.
- `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt` — register flyer-template subroutes inside `orgsPackRoutes`.
- `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/bindings/SponsoringModule.kt` — bind `FlyerTemplateRepository`.
- `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt` — install new generate route alongside support-video routes.
- `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/bindings/PartnershipModule.kt` — bind `FlyerGenerationRepository`.
- `application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/NotificationVariables.kt` — add `FlyerGenerated` data class.
- `application/src/main/kotlin/fr/devlille/partners/connect/digest/domain/EventDigest.kt` — add `flyerItems` field + extend `hasItems`.
- `application/src/main/kotlin/fr/devlille/partners/connect/digest/application/DigestRepositoryExposed.kt` — add `queryFlyerEligible`.
- `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/MigrationRegistry.kt` — register new migration.
- `application/src/main/resources/openapi/openapi.yaml` — three new endpoints.
- `application/src/main/resources/schemas/upload_flyer_template.schema.json` — new schema for zone JSON part.
- `application/src/main/resources/notifications/email/flyer_generated/{en,fr}.md` — new templates.
- `application/src/main/resources/notifications/slack/flyer_generated/{en,fr}.md` — new templates.

**New files (production):**

- `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/AddFlyerTemplateColumnsToSponsoringPacksMigration.kt`
- `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/FlyerTemplate.kt` — domain value type `FlyerTemplate(templateUrl, zone)` and `FlyerZone(x, y, width, height)`.
- `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/FlyerTemplateRepository.kt` — interface.
- `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/FlyerTemplateRepositoryExposed.kt` — implementation (uses `Storage` directly for the template file).
- `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/FlyerGenerationRepository.kt` — interface.
- `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/FlyerGenerationRepositoryImpl.kt` — orchestrates pre-conditions + composition + storage.
- `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/FlyerComposer.kt` — pure JPG-bytes-from-template-bytes-and-logo-bytes function (unit-testable, no I/O).
- `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerRoutes.kt` — `orgsPartnershipFlyerRoutes()` function.

**New files (tests):**

- `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/factories/SponsoringPackEntity.factory.kt` — extend existing factory (no new file) to accept flyer params.
- `application/src/test/kotlin/fr/devlille/partners/connect/partnership/application/FlyerComposerTest.kt` — pure unit tests (no DB).
- `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/OrgsPackFlyerTemplatePutRouteTest.kt`
- `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/OrgsPackFlyerTemplateDeleteRouteTest.kt`
- `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerGenerateRoutePostTest.kt`
- `application/src/test/kotlin/fr/devlille/partners/connect/digest/application/DigestFlyerEligibilityTest.kt`

---

## Task 1: Schema migration + table columns + entity properties

**Files:**
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringPacksTable.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringPackEntity.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/AddFlyerTemplateColumnsToSponsoringPacksMigration.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/MigrationRegistry.kt`

- [ ] **Step 1.1 — Add nullable columns to `SponsoringPacksTable`**

Replace the contents of `SponsoringPacksTable.kt` with:

```kotlin
@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.sponsoring.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object SponsoringPacksTable : UUIDTable("sponsoring_packs") {
    val eventId = reference("event_id", EventsTable)
    val name = varchar("name", 255)
    val basePrice = integer("base_price")
    val withBooth = bool("with_booth").default(false)
    val nbTickets = integer("nb_ticket").default(0)
    val maxQuantity = integer("max_quantity").nullable()
    val flyerTemplateUrl = text("flyer_template_url").nullable()
    val flyerZoneX = integer("flyer_zone_x").nullable()
    val flyerZoneY = integer("flyer_zone_y").nullable()
    val flyerZoneWidth = integer("flyer_zone_width").nullable()
    val flyerZoneHeight = integer("flyer_zone_height").nullable()
}
```

- [ ] **Step 1.2 — Add entity properties + `hasFlyerTemplate()` helper**

In `SponsoringPackEntity.kt`, add five `var` mappings inside the class body (under `var maxQuantity`):

```kotlin
var flyerTemplateUrl by SponsoringPacksTable.flyerTemplateUrl
var flyerZoneX by SponsoringPacksTable.flyerZoneX
var flyerZoneY by SponsoringPacksTable.flyerZoneY
var flyerZoneWidth by SponsoringPacksTable.flyerZoneWidth
var flyerZoneHeight by SponsoringPacksTable.flyerZoneHeight
```

Then append, after the existing top-level functions:

```kotlin
/**
 * A pack is "flyer-enabled" only when the template URL and all four zone coordinates are non-null.
 */
fun SponsoringPackEntity.hasFlyerTemplate(): Boolean =
    flyerTemplateUrl != null &&
        flyerZoneX != null &&
        flyerZoneY != null &&
        flyerZoneWidth != null &&
        flyerZoneHeight != null
```

- [ ] **Step 1.3 — Create the migration**

Create `AddFlyerTemplateColumnsToSponsoringPacksMigration.kt`:

```kotlin
package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddFlyerTemplateColumnsToSponsoringPacksMigration : Migration {
    override val id = "20260516_add_flyer_template_columns_to_sponsoring_packs"
    override val description = "Add nullable flyer_template_url and flyer_zone_x/y/width/height columns to sponsoring_packs"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(SponsoringPacksTable)
    }
}
```

- [ ] **Step 1.4 — Register the migration**

In `MigrationRegistry.kt`, add the import alphabetically and append the migration at the end of `allMigrations`:

```kotlin
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddFlyerTemplateColumnsToSponsoringPacksMigration
```

Then in the `allMigrations` list, add as the last entry:

```kotlin
        CreatePartnershipSupportVideosTableMigration,
        AddFlyerTemplateColumnsToSponsoringPacksMigration,
    )
```

- [ ] **Step 1.5 — Compile to verify**

Run: `./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 1.6 — Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringPacksTable.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/db/SponsoringPackEntity.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/versions/AddFlyerTemplateColumnsToSponsoringPacksMigration.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/migrations/MigrationRegistry.kt
git commit -m "feat(server): add nullable flyer_template columns to sponsoring_packs"
```

---

## Task 2: Domain types `FlyerTemplate` and `FlyerZone`

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/FlyerTemplate.kt`

- [ ] **Step 2.1 — Write the domain types**

Create `FlyerTemplate.kt`:

```kotlin
package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.Serializable

@Serializable
data class FlyerZone(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

data class FlyerTemplate(
    val templateUrl: String,
    val zone: FlyerZone,
)
```

- [ ] **Step 2.2 — Compile + commit**

Run: `./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/FlyerTemplate.kt
git commit -m "feat(server): add FlyerTemplate and FlyerZone domain types"
```

---

## Task 3: Extend `SponsoringPackEntity.factory.kt` to accept flyer params

This unblocks all later contract tests that need a flyer-enabled pack.

**Files:**
- Modify: `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/factories/SponsoringPackEntity.factory.kt`

- [ ] **Step 3.1 — Read the existing factory**

Run: `cat application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/factories/SponsoringPackEntity.factory.kt`

Expected: a function `insertMockedSponsoringPack(packId, eventId, ...)` that calls `SponsoringPackEntity.new(packId) { ... }`.

- [ ] **Step 3.2 — Add five optional flyer parameters**

Add to the parameter list (with defaults `null`):

```kotlin
flyerTemplateUrl: String? = null,
flyerZoneX: Int? = null,
flyerZoneY: Int? = null,
flyerZoneWidth: Int? = null,
flyerZoneHeight: Int? = null,
```

Inside the `SponsoringPackEntity.new(...) { ... }` block, append:

```kotlin
this.flyerTemplateUrl = flyerTemplateUrl
this.flyerZoneX = flyerZoneX
this.flyerZoneY = flyerZoneY
this.flyerZoneWidth = flyerZoneWidth
this.flyerZoneHeight = flyerZoneHeight
```

- [ ] **Step 3.3 — Add a convenience helper for flyer-enabled packs**

Append at the bottom of the file:

```kotlin
fun insertMockedFlyerEnabledPack(
    packId: java.util.UUID,
    eventId: java.util.UUID,
    templateUrl: String = "https://storage.googleapis.com/test-bucket/events/$eventId/packs/$packId/flyer-template.png",
    zoneX: Int = 100,
    zoneY: Int = 200,
    zoneWidth: Int = 800,
    zoneHeight: Int = 500,
) = insertMockedSponsoringPack(
    id = packId,
    eventId = eventId,
    flyerTemplateUrl = templateUrl,
    flyerZoneX = zoneX,
    flyerZoneY = zoneY,
    flyerZoneWidth = zoneWidth,
    flyerZoneHeight = zoneHeight,
)
```

(Match the existing factory's parameter names — re-read the file before editing to make sure the parameter named `id` here matches what the factory exposes.)

- [ ] **Step 3.4 — Compile tests + commit**

Run: `./gradlew :application:compileTestKotlin`
Expected: BUILD SUCCESSFUL

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/factories/SponsoringPackEntity.factory.kt
git commit -m "test(server): extend pack factory with flyer template params"
```

---

## Task 4: `FlyerTemplateRepository` — read pack template + persist + delete

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/FlyerTemplateRepository.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/FlyerTemplateRepositoryExposed.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/bindings/SponsoringModule.kt`

- [ ] **Step 4.1 — Define the domain interface**

Create `FlyerTemplateRepository.kt`:

```kotlin
package fr.devlille.partners.connect.sponsoring.domain

import java.util.UUID

interface FlyerTemplateRepository {
    /**
     * Returns the configured flyer template for the given pack, or null when the pack is not flyer-enabled.
     */
    fun get(eventSlug: String, packId: UUID): FlyerTemplate?

    /**
     * Persists the PNG template bytes into storage and writes the five flyer columns on the pack.
     * Replaces the existing template file if one is configured. Throws if the pack does not belong to the event.
     */
    fun save(eventSlug: String, packId: UUID, pngBytes: ByteArray, zone: FlyerZone): FlyerTemplate

    /**
     * Clears all five flyer columns on the pack and deletes the underlying template file.
     * No-op if the pack is not flyer-enabled.
     */
    fun clear(eventSlug: String, packId: UUID)
}
```

- [ ] **Step 4.2 — Implement against Exposed + Storage**

Create `FlyerTemplateRepositoryExposed.kt`:

```kotlin
package fr.devlille.partners.connect.sponsoring.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.bucket.MimeType
import fr.devlille.partners.connect.internal.infrastructure.bucket.Storage
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplate
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.hasFlyerTemplate
import fr.devlille.partners.connect.sponsoring.infrastructure.db.singlePackById
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class FlyerTemplateRepositoryExposed(
    private val storage: Storage,
) : FlyerTemplateRepository {
    override fun get(eventSlug: String, packId: UUID): FlyerTemplate? = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        if (!pack.hasFlyerTemplate()) {
            null
        } else {
            FlyerTemplate(
                templateUrl = pack.flyerTemplateUrl!!,
                zone = FlyerZone(
                    x = pack.flyerZoneX!!,
                    y = pack.flyerZoneY!!,
                    width = pack.flyerZoneWidth!!,
                    height = pack.flyerZoneHeight!!,
                ),
            )
        }
    }

    override fun save(
        eventSlug: String,
        packId: UUID,
        pngBytes: ByteArray,
        zone: FlyerZone,
    ): FlyerTemplate = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        val filename = "events/${event.id.value}/packs/$packId/flyer-template.png"
        val upload = storage.upload(filename, pngBytes, MimeType.PNG)
        pack.flyerTemplateUrl = upload.url
        pack.flyerZoneX = zone.x
        pack.flyerZoneY = zone.y
        pack.flyerZoneWidth = zone.width
        pack.flyerZoneHeight = zone.height
        FlyerTemplate(templateUrl = upload.url, zone = zone)
    }

    override fun clear(eventSlug: String, packId: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
        if (pack.hasFlyerTemplate()) {
            storage.delete("events/${event.id.value}/packs/$packId/flyer-template.png")
            pack.flyerTemplateUrl = null
            pack.flyerZoneX = null
            pack.flyerZoneY = null
            pack.flyerZoneWidth = null
            pack.flyerZoneHeight = null
        }
        Unit
    }
}
```

- [ ] **Step 4.3 — Bind in SponsoringModule**

Edit `SponsoringModule.kt`:

```kotlin
package fr.devlille.partners.connect.sponsoring.infrastructure.bindings

import fr.devlille.partners.connect.sponsoring.application.FlyerTemplateRepositoryExposed
import fr.devlille.partners.connect.sponsoring.application.OptionRepositoryExposed
import fr.devlille.partners.connect.sponsoring.application.PackRepositoryExposed
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import org.koin.dsl.module

val sponsoringModule = module {
    single<PackRepository> { PackRepositoryExposed() }
    single<OptionRepository> { OptionRepositoryExposed() }
    single<FlyerTemplateRepository> { FlyerTemplateRepositoryExposed(storage = get()) }
}
```

- [ ] **Step 4.4 — Compile + commit**

Run: `./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/domain/FlyerTemplateRepository.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/FlyerTemplateRepositoryExposed.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/bindings/SponsoringModule.kt
git commit -m "feat(server): add FlyerTemplateRepository for pack flyer configuration"
```

---

## Task 5: JSON schema for the zone payload + multipart helpers

**Files:**
- Create: `application/src/main/resources/schemas/upload_flyer_template.schema.json`

- [ ] **Step 5.1 — Create the schema**

```json
{
  "$id": "upload_flyer_template.schema.json",
  "type": "object",
  "properties": {
    "x": { "type": "integer", "minimum": 0 },
    "y": { "type": "integer", "minimum": 0 },
    "width": { "type": "integer", "minimum": 1 },
    "height": { "type": "integer", "minimum": 1 }
  },
  "required": ["x", "y", "width", "height"]
}
```

- [ ] **Step 5.2 — Commit**

```bash
git add application/src/main/resources/schemas/upload_flyer_template.schema.json
git commit -m "feat(server): add upload_flyer_template JSON schema for zone payload"
```

---

## Task 6: PUT route — upload pack flyer template

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/OrgsPackFlyerTemplatePutRouteTest.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt`

The endpoint accepts multipart with two parts: a `file` (PNG) and a `zone` (JSON `{x,y,width,height}`). Validates: PNG MIME, parseable image, zone fits inside template dimensions.

- [ ] **Step 6.1 — Write failing contract test for happy path**

Create `OrgsPackFlyerTemplatePutRouteTest.kt`:

```kotlin
package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
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
    fun `organiser uploads flyer template and zone`() = testApplication {
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

        val template = pngBytes(1200, 800)
        val response = client.put("/orgs/$orgId/events/$eventId/packs/$packId/flyer-template") {
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = template,
                            headers = headersOf(
                                HttpHeaders.ContentType to listOf(ContentType.Image.PNG.toString()),
                                HttpHeaders.ContentDisposition to listOf("form-data; name=\"file\"; filename=\"t.png\""),
                            ),
                        )
                        append(
                            key = "zone",
                            value = "{\"x\":100,\"y\":200,\"width\":800,\"height\":500}",
                            headers = headersOf(
                                HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                            ),
                        )
                    },
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("flyer-template.png"), "Response body should contain stored URL")
    }

    @Test
    fun `upload rejects non-PNG file with 415`() = testApplication {
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

        val response = client.put("/orgs/$orgId/events/$eventId/packs/$packId/flyer-template") {
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = ByteArray(10),
                            headers = headersOf(
                                HttpHeaders.ContentType to listOf("image/jpeg"),
                                HttpHeaders.ContentDisposition to listOf("form-data; name=\"file\"; filename=\"t.jpg\""),
                            ),
                        )
                        append(key = "zone", value = "{\"x\":0,\"y\":0,\"width\":10,\"height\":10}")
                    },
                ),
            )
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `upload rejects zone outside template bounds with 400`() = testApplication {
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
        val response = client.put("/orgs/$orgId/events/$eventId/packs/$packId/flyer-template") {
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = template,
                            headers = headersOf(
                                HttpHeaders.ContentType to listOf("image/png"),
                                HttpHeaders.ContentDisposition to listOf("form-data; name=\"file\"; filename=\"t.png\""),
                            ),
                        )
                        append(key = "zone", value = "{\"x\":50,\"y\":50,\"width\":80,\"height\":80}")
                    },
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `upload returns 401 without auth`() = testApplication {
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

        val response = client.put("/orgs/$orgId/events/$eventId/packs/$packId/flyer-template") {
            setBody(MultiPartFormDataContent(formData { append("file", ByteArray(0)) }))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
```

- [ ] **Step 6.2 — Run the test, expect failure**

Run: `./gradlew :application:test --tests "*OrgsPackFlyerTemplatePutRouteTest*"`
Expected: FAIL (route not defined → 404 or 405).

- [ ] **Step 6.3 — Add the route under `orgsPackRoutes`**

In `SponsoringRoutes.kt`, inside `orgsPackRoutes`'s `route("/orgs/{orgSlug}/events/{eventSlug}/packs") { ... }` block, after the existing `delete("/{packId}")` route, add:

```kotlin
put("/{packId}/flyer-template") {
    val eventSlug = call.parameters.eventSlug
    val packId = call.parameters.packId
    val (pngBytes, zone) = receiveFlyerTemplateUpload(call)
    val saved = flyerTemplateRepository.save(eventSlug, packId, pngBytes, zone)
    call.respond(HttpStatusCode.OK, mapOf("template_url" to saved.templateUrl))
}
delete("/{packId}/flyer-template") {
    val eventSlug = call.parameters.eventSlug
    val packId = call.parameters.packId
    flyerTemplateRepository.clear(eventSlug, packId)
    call.respond(HttpStatusCode.NoContent)
}
```

At the top of `orgsPackRoutes`, alongside existing `inject` calls, add:

```kotlin
val flyerTemplateRepository by inject<FlyerTemplateRepository>()
```

Add the relevant imports at the top of the file:

```kotlin
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
```

- [ ] **Step 6.4 — Implement `receiveFlyerTemplateUpload`**

At the bottom of `SponsoringRoutes.kt`, add a private helper:

```kotlin
private suspend fun receiveFlyerTemplateUpload(call: ApplicationCall): Pair<ByteArray, FlyerZone> {
    val multipart = call.receiveMultipart()
    var pngBytes: ByteArray? = null
    var zoneJson: String? = null
    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                val mime = part.contentType?.toString()
                if (mime != "image/png") {
                    part.dispose()
                    throw UnsupportedMediaTypeException("Unsupported template type: $mime — PNG only")
                }
                pngBytes = part.asByteArray()
            }
            is PartData.FormItem -> {
                if (part.name == "zone") zoneJson = part.value
            }
            else -> Unit
        }
        part.dispose()
    }
    val bytes = pngBytes ?: throw MissingRequestParameterException("file")
    val raw = zoneJson ?: throw MissingRequestParameterException("zone")
    val zone = Json.decodeFromString(FlyerZone.serializer(), raw)
    validatePngFitsZone(bytes, zone)
    return bytes to zone
}

private fun validatePngFitsZone(pngBytes: ByteArray, zone: FlyerZone) {
    val image = ImageIO.read(ByteArrayInputStream(pngBytes))
        ?: throw BadRequestException("Uploaded file is not a readable PNG image")
    if (zone.x < 0 || zone.y < 0 || zone.width <= 0 || zone.height <= 0) {
        throw BadRequestException("Zone coordinates must be non-negative with positive width/height")
    }
    if (zone.x + zone.width > image.width || zone.y + zone.height > image.height) {
        throw BadRequestException(
            "Zone ($zone) does not fit inside template (${image.width}x${image.height})",
        )
    }
}
```

Imports to add at the top of the file:

```kotlin
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.receiveMultipart
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
```

- [ ] **Step 6.5 — Run tests, expect pass**

Run: `./gradlew :application:test --tests "*OrgsPackFlyerTemplatePutRouteTest*"`
Expected: PASS for all four scenarios.

- [ ] **Step 6.6 — Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/SponsoringRoutes.kt \
        application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/OrgsPackFlyerTemplatePutRouteTest.kt
git commit -m "feat(server): PUT /orgs/.../packs/{packId}/flyer-template route"
```

---

## Task 7: DELETE route — clear pack flyer template

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/OrgsPackFlyerTemplateDeleteRouteTest.kt`

The DELETE route was already wired up in Task 6 alongside PUT. This task just adds the contract test.

- [ ] **Step 7.1 — Write the contract test**

```kotlin
package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedFlyerEnabledPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.hasFlyerTemplate
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
import kotlin.test.assertFalse

class OrgsPackFlyerTemplateDeleteRouteTest {
    @Test
    fun `delete clears all five flyer columns`() = testApplication {
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
                insertMockedFlyerEnabledPack(packId = packId, eventId = eventId)
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/packs/$packId/flyer-template") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        transaction {
            val pack = SponsoringPackEntity[packId]
            assertFalse(pack.hasFlyerTemplate(), "Flyer columns must be null after delete")
        }
    }

    @Test
    fun `delete returns 401 without auth`() = testApplication {
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

        val response = client.delete("/orgs/$orgId/events/$eventId/packs/$packId/flyer-template")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
```

- [ ] **Step 7.2 — Run + commit**

Run: `./gradlew :application:test --tests "*OrgsPackFlyerTemplateDeleteRouteTest*"`
Expected: PASS.

```bash
git add application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/infrastructure/api/OrgsPackFlyerTemplateDeleteRouteTest.kt
git commit -m "test(server): contract test for DELETE pack flyer-template"
```

---

## Task 8: `FlyerComposer` — pure JPG composition function (TDD)

This is the most testable unit: takes template bytes + logo bytes + zone, returns JPG bytes. No I/O. Mirrors the algorithm in `communication-generator-kotlin/src/main/kotlin/com/devlille/communication/partners/FlyerGenerator.kt` lines 72-112, but uses `imgscalr.Scalr.resize` instead of manual rendering hints.

**Files:**
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/partnership/application/FlyerComposerTest.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/FlyerComposer.kt`

- [ ] **Step 8.1 — Write failing unit tests**

```kotlin
package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlyerComposerTest {
    private fun pngBytes(width: Int, height: Int, fill: Color = Color.WHITE): ByteArray {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = fill
        g.fillRect(0, 0, width, height)
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        return out.toByteArray()
    }

    @Test
    fun `compose returns a readable JPG with same dimensions as the template`() {
        val template = pngBytes(1200, 800)
        val logo = pngBytes(400, 400, Color.RED)
        val zone = FlyerZone(x = 100, y = 200, width = 800, height = 500)

        val jpgBytes = FlyerComposer.compose(template, logo, zone)
        val rendered = ImageIO.read(ByteArrayInputStream(jpgBytes))

        assertEquals(1200, rendered.width)
        assertEquals(800, rendered.height)
    }

    @Test
    fun `compose draws the logo only within the configured zone respecting a 20px margin`() {
        val template = pngBytes(1200, 800, Color.WHITE)
        val logo = pngBytes(400, 400, Color.RED)
        val zone = FlyerZone(x = 100, y = 200, width = 800, height = 500)

        val jpgBytes = FlyerComposer.compose(template, logo, zone)
        val rendered = ImageIO.read(ByteArrayInputStream(jpgBytes))

        // Pixels just outside the zone (at zone edge) should remain template-white (not red).
        val outsidePixel = Color(rendered.getRGB(zone.x - 1, zone.y - 1))
        assertEquals(255, outsidePixel.red)
        assertEquals(255, outsidePixel.green)
        assertEquals(255, outsidePixel.blue)

        // Inside the margin (within 20px of zone edge) should also stay template-white.
        val marginPixel = Color(rendered.getRGB(zone.x + 5, zone.y + 5))
        assertEquals(255, marginPixel.red)
        assertEquals(255, marginPixel.green)
        assertEquals(255, marginPixel.blue)

        // Centre of the zone should be red (logo drawn there).
        val centrePixel = Color(rendered.getRGB(zone.x + zone.width / 2, zone.y + zone.height / 2))
        assertTrue(centrePixel.red > 200, "Centre pixel should be predominantly red")
        assertTrue(centrePixel.green < 80, "Centre pixel green channel should be low")
    }

    @Test
    fun `compose preserves logo aspect ratio when it is wider than tall`() {
        val template = pngBytes(1200, 800)
        val logo = pngBytes(800, 200, Color.RED) // 4:1 aspect
        val zone = FlyerZone(x = 100, y = 200, width = 800, height = 500)

        val jpgBytes = FlyerComposer.compose(template, logo, zone)
        val rendered = ImageIO.read(ByteArrayInputStream(jpgBytes))

        // Logo width should fit zone width minus 2*margin (800 - 40 = 760), height proportional.
        // Top of zone (well above the centred logo) should still be white.
        val topOfZone = Color(rendered.getRGB(zone.x + zone.width / 2, zone.y + 5))
        assertEquals(255, topOfZone.red)
        assertEquals(255, topOfZone.green)
    }
}
```

- [ ] **Step 8.2 — Run the tests, expect failure**

Run: `./gradlew :application:test --tests "*FlyerComposerTest*"`
Expected: FAIL with `FlyerComposer` unresolved.

- [ ] **Step 8.3 — Implement `FlyerComposer`**

```kotlin
package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object FlyerComposer {
    private const val ZONE_MARGIN_PX = 20

    /**
     * Composes the partner's logo into the template's configured zone and returns JPG bytes.
     * The logo is scaled (preserving aspect ratio) to fit inside the zone minus a 20px margin
     * on each side, and centred within the zone. Template dimensions are preserved.
     */
    fun compose(templatePng: ByteArray, logo: ByteArray, zone: FlyerZone): ByteArray {
        val templateImg = ImageIO.read(ByteArrayInputStream(templatePng))
            ?: error("Template bytes are not a readable image")
        val logoImg = ImageIO.read(ByteArrayInputStream(logo))
            ?: error("Logo bytes are not a readable image")

        val availableWidth = zone.width - (ZONE_MARGIN_PX * 2)
        val availableHeight = zone.height - (ZONE_MARGIN_PX * 2)
        val resized = scaleToFit(logoImg, availableWidth, availableHeight)

        val xPosition = zone.x + (zone.width - resized.width) / 2
        val yPosition = zone.y + (zone.height - resized.height) / 2

        val output = BufferedImage(templateImg.width, templateImg.height, BufferedImage.TYPE_INT_RGB)
        val g = output.createGraphics()
        g.drawImage(templateImg, 0, 0, null)
        g.drawImage(resized, xPosition, yPosition, null)
        g.dispose()

        val out = ByteArrayOutputStream()
        ImageIO.write(output, "jpg", out)
        return out.toByteArray()
    }

    private fun scaleToFit(logo: BufferedImage, availableWidth: Int, availableHeight: Int): BufferedImage {
        val aspect = logo.width.toDouble() / logo.height.toDouble()
        var targetWidth = availableWidth
        var targetHeight = (targetWidth / aspect).toInt()
        if (targetHeight > availableHeight) {
            targetHeight = availableHeight
            targetWidth = (targetHeight * aspect).toInt()
        }
        return Scalr.resize(logo, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, targetWidth, targetHeight)
    }
}
```

- [ ] **Step 8.4 — Run tests, expect pass**

Run: `./gradlew :application:test --tests "*FlyerComposerTest*"`
Expected: PASS.

- [ ] **Step 8.5 — Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/FlyerComposer.kt \
        application/src/test/kotlin/fr/devlille/partners/connect/partnership/application/FlyerComposerTest.kt
git commit -m "feat(server): add pure FlyerComposer with aspect-preserving zone fit"
```

---

## Task 9: `FlyerGenerationRepository` — orchestrate preconditions, download, compose, persist

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/FlyerGenerationRepository.kt`
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/FlyerGenerationRepositoryImpl.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/bindings/PartnershipModule.kt`

The repository must produce a `GeneratedFlyer(url)` and overwrite `partnership.communicationSupportUrl` in the same transaction. The HTTP client used to fetch the template + logo is the shared one (`networkClientModule` already supplies `HttpClient`).

- [ ] **Step 9.1 — Define the result type and interface**

Create `FlyerGenerationRepository.kt`:

```kotlin
package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

data class GeneratedFlyer(val url: String)

interface FlyerGenerationRepository {
    /**
     * Generates a flyer for the given partnership and stores the URL on the partnership's
     * communication_support_url column. Throws:
     *  - NotFoundException if the partnership or pack is not found.
     *  - ConflictException if the partnership is not validated, the company has no logo,
     *    or the pack is not flyer-enabled.
     *  - IOException if the template or logo download fails.
     */
    suspend fun generate(eventSlug: String, partnershipId: UUID): GeneratedFlyer
}
```

- [ ] **Step 9.2 — Implement the orchestrator**

Create `FlyerGenerationRepositoryImpl.kt`:

```kotlin
package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.partnership.domain.FlyerGenerationRepository
import fr.devlille.partners.connect.partnership.domain.GeneratedFlyer
import fr.devlille.partners.connect.partnership.domain.PartnershipStorageRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplate
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class FlyerGenerationRepositoryImpl(
    private val httpClient: HttpClient,
    private val flyerTemplateRepository: FlyerTemplateRepository,
    private val partnershipStorageRepository: PartnershipStorageRepository,
) : FlyerGenerationRepository {

    override suspend fun generate(eventSlug: String, partnershipId: UUID): GeneratedFlyer {
        val context = transaction {
            val event = EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
            val partnership = PartnershipEntity.findById(partnershipId)
                ?: throw NotFoundException("Partnership $partnershipId not found")
            if (partnership.event.id != event.id) {
                throw NotFoundException("Partnership $partnershipId not found in event $eventSlug")
            }
            if (partnership.validatedAt == null) {
                throw ConflictException("Partnership must be validated before generating a flyer")
            }
            val pack = partnership.selectedPack
                ?: throw ConflictException("Partnership has no selected pack")
            val template = flyerTemplateRepository.get(eventSlug, pack.id.value)
                ?: throw ConflictException("Pack ${pack.id.value} is not flyer-enabled")
            val logoUrl = partnership.company.logoUrl1000 ?: partnership.company.logoUrlOriginal
                ?: throw ConflictException("Company has no logo")
            GenerationContext(template = template, logoUrl = logoUrl)
        }

        val templateBytes = httpClient.get(context.template.templateUrl).readBytes()
        val logoBytes = httpClient.get(context.logoUrl).readBytes()
        val jpgBytes = FlyerComposer.compose(templateBytes, logoBytes, context.template.zone)

        val storedUrl = partnershipStorageRepository.uploadCommunicationSupport(
            eventSlug = eventSlug,
            partnershipId = partnershipId,
            content = jpgBytes,
            mimeType = "image/jpeg",
        )

        transaction {
            val partnership = PartnershipEntity[partnershipId]
            partnership.communicationSupportUrl = storedUrl
        }

        return GeneratedFlyer(url = storedUrl)
    }

    private data class GenerationContext(val template: FlyerTemplate, val logoUrl: String)
}
```

Note: confirm `PartnershipEntity` exposes `communicationSupportUrl` as a writable `var`. If the existing entity only has it as a `val` derived from the table, expose it as `var` in `PartnershipEntity.kt` first — but most likely it already mirrors `PartnershipsTable.communicationSupportUrl` since other code writes to it.

- [ ] **Step 9.3 — Confirm entity mapping (no edit expected)**

`PartnershipEntity.kt` already exposes `var communicationSupportUrl by PartnershipsTable.communicationSupportUrl` (verified at the time this plan was written). No code change needed in this step — just compile to be sure.

Run: `./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9.4 — Bind in `PartnershipModule`**

Open `PartnershipModule.kt`. Inside the module block, add:

```kotlin
single<FlyerGenerationRepository> {
    FlyerGenerationRepositoryImpl(
        httpClient = get(),
        flyerTemplateRepository = get(),
        partnershipStorageRepository = get(),
    )
}
```

Imports to add:

```kotlin
import fr.devlille.partners.connect.partnership.application.FlyerGenerationRepositoryImpl
import fr.devlille.partners.connect.partnership.domain.FlyerGenerationRepository
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
```

Also ensure `sponsoringModule` is included (`includes(sponsoringModule)`) if it isn't already. If unsure, run:

`grep -n "includes\|sponsoring" application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/bindings/PartnershipModule.kt`

If `sponsoringModule` is not included there, ensure both modules are loaded by the app (read `application/src/main/kotlin/fr/devlille/partners/connect/App.kt` or the `Application.module` function — they are typically loaded as siblings, so no include is required).

- [ ] **Step 9.5 — Compile + commit**

Run: `./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/FlyerGenerationRepository.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/FlyerGenerationRepositoryImpl.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/bindings/PartnershipModule.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipEntity.kt
git commit -m "feat(server): add FlyerGenerationRepository orchestrator"
```

---

## Task 10: POST route — generate a partnership flyer (no notifications yet)

This task wires the route, returns the URL, and verifies the pre-condition matrix. Notifications come in Task 12.

**Files:**
- Create: `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerRoutes.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt`
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerGenerateRoutePostTest.kt`

- [ ] **Step 10.1 — Write failing contract tests (pre-condition matrix)**

Create `PartnershipFlyerGenerateRoutePostTest.kt`:

```kotlin
package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedCompanyWithoutLogo
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedFlyerEnabledPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipFlyerGenerateRoutePostTest {
    @Test
    fun `generate returns 409 when partnership is not validated`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedFlyerEnabledPack(packId = packId, eventId = eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/flyer") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `generate returns 409 when company has no logo`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompanyWithoutLogo(companyId)
                insertMockedFlyerEnabledPack(packId = packId, eventId = eventId)
                insertMockedValidatedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/flyer") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `generate returns 409 when pack is not flyer-enabled`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
                insertMockedValidatedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/flyer") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `generate returns 401 without auth`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedFlyerEnabledPack(packId = packId, eventId = eventId)
                insertMockedValidatedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/flyer")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
```

This test set requires two new factory helpers:
- `insertMockedCompanyWithoutLogo(companyId)`
- `insertMockedValidatedPartnership(id, eventId, companyId, selectedPackId)`

- [ ] **Step 10.2 — Add the missing test factories**

Open `application/src/test/kotlin/fr/devlille/partners/connect/companies/factories/Company.factory.kt` (file name may vary — locate via `find application/src/test -name "Company*.factory.kt"`). After the existing `insertMockedCompany`, add:

```kotlin
fun insertMockedCompanyWithoutLogo(id: java.util.UUID): CompanyEntity =
    insertMockedCompany(id).apply {
        logoUrlOriginal = null
        logoUrl1000 = null
        logoUrl500 = null
        logoUrl250 = null
    }
```

(Adjust property names to match the actual entity — verify with `grep -n "logoUrl" application/src/main/kotlin/fr/devlille/partners/connect/companies/infrastructure/db/CompanyEntity.kt`.)

Open `application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/Partnership.factory.kt`. Below the existing `insertMockedPartnership`, add:

```kotlin
@Suppress("LongParameterList")
fun insertMockedValidatedPartnership(
    id: java.util.UUID,
    eventId: java.util.UUID,
    companyId: java.util.UUID,
    selectedPackId: java.util.UUID,
): PartnershipEntity = insertMockedPartnership(
    id = id,
    eventId = eventId,
    companyId = companyId,
    selectedPackId = selectedPackId,
).apply {
    validatedAt = kotlinx.datetime.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
}
```

- [ ] **Step 10.3 — Run the tests, expect failure**

Run: `./gradlew :application:test --tests "*PartnershipFlyerGenerateRoutePostTest*"`
Expected: FAIL — route is not yet wired.

- [ ] **Step 10.4 — Create the route file**

Create `PartnershipFlyerRoutes.kt`:

```kotlin
package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.partnership.domain.FlyerGenerationRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.orgsPartnershipFlyerRoutes() {
    val repository by inject<FlyerGenerationRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/flyer") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val flyer = repository.generate(eventSlug, partnershipId)
            call.respond(HttpStatusCode.OK, mapOf("url" to flyer.url))
        }
    }
}
```

- [ ] **Step 10.5 — Install the route**

In `PartnershipRoutes.kt`, find the existing function that aggregates partnership routes (alongside `publicPartnershipSupportVideoRoutes()` / `orgsPartnershipSupportVideoRoutes()`). Add a call to `orgsPartnershipFlyerRoutes()`. Add the import.

- [ ] **Step 10.6 — Run tests, expect pass**

Run: `./gradlew :application:test --tests "*PartnershipFlyerGenerateRoutePostTest*"`
Expected: PASS for all four scenarios.

- [ ] **Step 10.7 — Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerRoutes.kt \
        application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt \
        application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerGenerateRoutePostTest.kt \
        application/src/test/kotlin/fr/devlille/partners/connect/companies/factories/ \
        application/src/test/kotlin/fr/devlille/partners/connect/partnership/factories/
git commit -m "feat(server): POST .../partnerships/{partnershipId}/flyer route"
```

---

## Task 11: Notification templates + `FlyerGenerated` variables

**Files:**
- Create: `application/src/main/resources/notifications/email/flyer_generated/en.md`
- Create: `application/src/main/resources/notifications/email/flyer_generated/fr.md`
- Create: `application/src/main/resources/notifications/slack/flyer_generated/en.md`
- Create: `application/src/main/resources/notifications/slack/flyer_generated/fr.md`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/NotificationVariables.kt`

- [ ] **Step 11.1 — Add the email templates**

`email/flyer_generated/en.md`:

```markdown
Hello,

Your flyer for {{event_name}} has been generated and is ready to share.

Company: {{company_name}}
Flyer: {{flyer_url}}

You can download it and use it on your channels at any time.

View partnership: {{partnership_link}}
```

`email/flyer_generated/fr.md`:

```markdown
Bonjour,

Votre flyer pour {{event_name}} a été généré et est prêt à être partagé.

Entreprise : {{company_name}}
Flyer : {{flyer_url}}

Vous pouvez le télécharger et l'utiliser sur vos canaux à tout moment.

Voir le partenariat : {{partnership_link}}
```

- [ ] **Step 11.2 — Add the Slack templates**

`slack/flyer_generated/en.md`:

```markdown
🎨 Flyer generated for *{{company_name}}* on *{{event_name}}*
Flyer: {{flyer_url}}
Partnership: {{partnership_link}}
```

`slack/flyer_generated/fr.md`:

```markdown
🎨 Flyer généré pour *{{company_name}}* sur *{{event_name}}*
Flyer : {{flyer_url}}
Partenariat : {{partnership_link}}
```

- [ ] **Step 11.3 — Add `FlyerGenerated` to `NotificationVariables`**

In `NotificationVariables.kt`, after `SupportVideoDeclined`, add:

```kotlin
data class FlyerGenerated(
    override val language: String,
    override val event: EventWithOrganisation,
    override val company: Company,
    val partnership: Partnership,
    val flyerUrl: String,
) : NotificationVariables {
    override val usageName: String = "flyer_generated"

    override fun populate(content: String): String = content
        .replace("{{event_name}}", event.event.name)
        .replace("{{event_contact}}", event.event.contact.email)
        .replace("{{company_name}}", company.name)
        .replace("{{flyer_url}}", flyerUrl)
        .replace("{{partnership_link}}", partnership.link(event))
}
```

- [ ] **Step 11.4 — Compile + commit**

Run: `./gradlew :application:compileKotlin`
Expected: BUILD SUCCESSFUL.

```bash
git add application/src/main/resources/notifications/email/flyer_generated/ \
        application/src/main/resources/notifications/slack/flyer_generated/ \
        application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/NotificationVariables.kt
git commit -m "feat(server): add FlyerGenerated notification templates + variables"
```

---

## Task 12: Wire notifications into the generate route

**Files:**
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerRoutes.kt`

- [ ] **Step 12.1 — Update the route to set notification variables**

Replace the body of the route block:

```kotlin
fun Route.orgsPartnershipFlyerRoutes() {
    val repository by inject<FlyerGenerationRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val eventRepository by inject<EventRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/flyer") {
        install(AuthorizedOrganisationPlugin)
        install(NotificationPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val flyer = repository.generate(eventSlug, partnershipId)

            val partnership = partnershipRepository.getById(eventSlug, partnershipId)
            call.attributes.variables = NotificationVariables.FlyerGenerated(
                language = partnership.language,
                event = eventRepository.getBySlug(eventSlug),
                company = partnershipRepository.getCompanyByPartnershipId(eventSlug, partnershipId),
                partnership = partnership,
                flyerUrl = flyer.url,
            )
            call.respond(HttpStatusCode.OK, mapOf("url" to flyer.url))
        }
    }
}
```

Imports to add:

```kotlin
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.ktor.NotificationPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.variables
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
```

- [ ] **Step 12.2 — Re-run the existing test to confirm no regression**

Run: `./gradlew :application:test --tests "*PartnershipFlyerGenerateRoutePostTest*"`
Expected: PASS — the NotificationPartnershipPlugin is benign in tests because the test environment uses no-op gateways (mirror the `support-video/approve` route, which installs the same plugin and is covered by tests).

If the test now fails because of a missing notification gateway in the test module, follow the same pattern used by `PartnershipSupportVideoApproveRoutePostTest`. Verify with: `grep -n "NotificationPartnershipPlugin" application/src/test/kotlin/fr/devlille/partners/connect/internal/ModuleSharedDb.kt` (or wherever `moduleSharedDb` lives) and confirm a test gateway is bound.

- [ ] **Step 12.3 — Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipFlyerRoutes.kt
git commit -m "feat(server): notify partners + organisers when flyer is generated"
```

---

## Task 13: Extend the morning digest with `flyerItems`

**Files:**
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/digest/domain/EventDigest.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/digest/application/DigestRepositoryExposed.kt`
- Modify: `application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/NotificationVariables.kt` (the `MorningDigest` class)
- Modify: `application/src/main/resources/notifications/slack/digest/en.md`
- Modify: `application/src/main/resources/notifications/slack/digest/fr.md`
- Create: `application/src/test/kotlin/fr/devlille/partners/connect/digest/application/DigestFlyerEligibilityTest.kt`

- [ ] **Step 13.1 — Write the failing eligibility test**

```kotlin
package fr.devlille.partners.connect.digest.application

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedCompanyWithoutLogo
import fr.devlille.partners.connect.digest.domain.DigestRepository
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedValidatedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedFlyerEnabledPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.test.KoinTest
import org.koin.test.inject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DigestFlyerEligibilityTest : KoinTest {
    @Test
    fun `digest flyerItems lists validated partnerships on flyer-enabled packs without a flyer yet`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "event-${UUID.randomUUID()}"
        val flyerPackId = UUID.randomUUID()
        val plainPackId = UUID.randomUUID()
        val eligibleCompanyId = UUID.randomUUID()
        val alreadyGeneratedCompanyId = UUID.randomUUID()
        val plainPackCompanyId = UUID.randomUUID()
        val notValidatedCompanyId = UUID.randomUUID()
        val eligiblePartnershipId = UUID.randomUUID()
        val alreadyGeneratedPartnershipId = UUID.randomUUID()
        val plainPackPartnershipId = UUID.randomUUID()
        val notValidatedPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, slug = eventSlug, orgId = orgId)
                insertMockedCompany(eligibleCompanyId)
                insertMockedCompany(alreadyGeneratedCompanyId)
                insertMockedCompany(plainPackCompanyId)
                insertMockedCompany(notValidatedCompanyId)
                insertMockedFlyerEnabledPack(packId = flyerPackId, eventId = eventId)
                insertMockedSponsoringPack(id = plainPackId, eventId = eventId)
                insertMockedValidatedPartnership(
                    id = eligiblePartnershipId,
                    eventId = eventId,
                    companyId = eligibleCompanyId,
                    selectedPackId = flyerPackId,
                )
                insertMockedValidatedPartnership(
                    id = alreadyGeneratedPartnershipId,
                    eventId = eventId,
                    companyId = alreadyGeneratedCompanyId,
                    selectedPackId = flyerPackId,
                ).apply { communicationSupportUrl = "https://example.com/flyer.jpg" }
                insertMockedValidatedPartnership(
                    id = plainPackPartnershipId,
                    eventId = eventId,
                    companyId = plainPackCompanyId,
                    selectedPackId = plainPackId,
                )
                insertMockedPartnership(
                    id = notValidatedPartnershipId,
                    eventId = eventId,
                    companyId = notValidatedCompanyId,
                    selectedPackId = flyerPackId,
                )
            }
        }

        val repository by inject<DigestRepository>()
        val digest = repository.queryDigest(eventSlug, LocalDate(2026, 5, 16))
        val flyerCompanies = digest.flyerItems.map { it.companyName }.toSet()
        assertEquals(1, digest.flyerItems.size, "Only the eligible partnership should be listed")
        assertTrue(flyerCompanies.size == 1, "Expected one company in flyerItems, got: $flyerCompanies")
    }
}
```

- [ ] **Step 13.2 — Run the test, expect failure**

Run: `./gradlew :application:test --tests "*DigestFlyerEligibilityTest*"`
Expected: FAIL — `flyerItems` does not exist on `EventDigest`.

- [ ] **Step 13.3 — Extend `EventDigest`**

Replace the contents:

```kotlin
package fr.devlille.partners.connect.digest.domain

import fr.devlille.partners.connect.events.domain.EventWithOrganisation

data class EventDigest(
    val event: EventWithOrganisation,
    val agreementItems: List<DigestEntry>,
    val billingItems: List<DigestEntry>,
    val socialMediaItems: List<DigestEntry>,
    val jobOfferItems: List<DigestEntry>,
    val supportVideoItems: List<DigestEntry>,
    val flyerItems: List<DigestEntry>,
) {
    val hasItems: Boolean
        get() = agreementItems.isNotEmpty() || billingItems.isNotEmpty() ||
            socialMediaItems.isNotEmpty() || jobOfferItems.isNotEmpty() ||
            supportVideoItems.isNotEmpty() || flyerItems.isNotEmpty()
}
```

- [ ] **Step 13.4 — Extend the `MorningDigest` notification variables**

In `NotificationVariables.kt`, add `val flyerItems: List<DigestEntry>` to `MorningDigest`'s constructor and add `.replace("{{flyer_section}}", formatSection(flyerItems, "n/a"))` to its `populate` body.

- [ ] **Step 13.5 — Update the digest Slack template files**

Append a `## Flyers à générer` (FR) / `## Flyers to generate` (EN) section using `{{flyer_section}}` to both `slack/digest/en.md` and `slack/digest/fr.md`. Verify the exact existing formatting first by reading those files.

- [ ] **Step 13.6 — Implement `queryFlyerEligible` and wire into `DigestRepositoryExposed`**

In `DigestRepositoryExposed.kt`, add inside the class:

```kotlin
private fun queryFlyerEligible(eventId: UUID, eventSlug: String): List<DigestEntry> =
    PartnershipEntity
        .find { PartnershipsTable.eventId eq eventId }
        .filter { partnership ->
            partnership.validatedAt != null &&
                partnership.communicationSupportUrl == null &&
                partnership.selectedPack?.hasFlyerTemplate() == true
        }
        .map { DigestEntry(it.company.name, buildLink(eventSlug, it.id.value)) }
```

Imports to add:

```kotlin
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.hasFlyerTemplate
```

In `queryDigest`'s `EventDigest(...)` constructor, add `flyerItems = queryFlyerEligible(eventEntity.id.value, eventSlug)`.

In `application/src/main/kotlin/fr/devlille/partners/connect/digest/infrastructure/api/DigestRoutes.kt:28` (the `NotificationVariables.MorningDigest(...)` constructor call), add `flyerItems = digest.flyerItems` as a constructor argument.

- [ ] **Step 13.7 — Run the test, expect pass**

Run: `./gradlew :application:test --tests "*DigestFlyerEligibilityTest*"`
Expected: PASS.

- [ ] **Step 13.8 — Run the existing digest tests to verify no regression**

Run: `./gradlew :application:test --tests "*Digest*"`
Expected: ALL PASS.

- [ ] **Step 13.9 — Commit**

```bash
git add application/src/main/kotlin/fr/devlille/partners/connect/digest/ \
        application/src/main/kotlin/fr/devlille/partners/connect/notifications/domain/NotificationVariables.kt \
        application/src/main/resources/notifications/slack/digest/ \
        application/src/test/kotlin/fr/devlille/partners/connect/digest/application/DigestFlyerEligibilityTest.kt
git commit -m "feat(server): include flyer-eligible partnerships in morning digest"
```

---

## Task 14: OpenAPI documentation

**Files:**
- Modify: `application/src/main/resources/openapi/openapi.yaml`

The existing file uses the `openapi-schemas` skill conventions. Follow them.

- [ ] **Step 14.1 — Add the three new paths**

Locate the `paths:` section. Add three new path entries (alphabetically ordered with siblings):

- `PUT /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/flyer-template` (multipart/form-data: `file` PNG, `zone` JSON; returns 200 `{template_url}` or 400/415/401/404)
- `DELETE /orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/flyer-template` (returns 204 or 401/404)
- `POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}/flyer` (no body; returns 200 `{url}` or 401/404/409)

Use the existing pack and partnership routes in the same file as the template for parameters, responses, and security schemas.

- [ ] **Step 14.2 — Validate**

Run: `npm run validate` (per `openapi-schemas` skill).
Expected: schema validation passes.

- [ ] **Step 14.3 — Bundle**

Run: `npm run bundle` (per `openapi-schemas` skill).
Expected: bundled documentation regenerated.

- [ ] **Step 14.4 — Commit**

```bash
git add application/src/main/resources/openapi/openapi.yaml application/src/main/resources/openapi/
git commit -m "docs(server): document flyer template and generation endpoints in OpenAPI"
```

---

## Task 15: Final verification

- [ ] **Step 15.1 — Run the full test suite**

Run: `./gradlew :application:test`
Expected: BUILD SUCCESSFUL — all tests pass, including the seven new test classes (`FlyerComposerTest`, `OrgsPackFlyerTemplatePutRouteTest`, `OrgsPackFlyerTemplateDeleteRouteTest`, `PartnershipFlyerGenerateRoutePostTest`, `DigestFlyerEligibilityTest`).

- [ ] **Step 15.2 — Run lint**

Run: `./gradlew :application:ktlintCheck :application:detekt`
Expected: zero violations. If any new lint warnings are emitted, fix inline (no `@Suppress` unless a rule already exists for similar code in the codebase).

- [ ] **Step 15.3 — Smoke-test the migration on a fresh database**

Run the server locally against an empty database (per project README) and confirm:
- The migration `20260516_add_flyer_template_columns_to_sponsoring_packs` runs without errors.
- Hitting `PUT /orgs/.../packs/.../flyer-template` with a real PNG via `curl` returns 200.
- Hitting `POST /orgs/.../partnerships/.../flyer` on a validated partnership returns 200 and `communication_support_url` is set in the DB.

If smoke tests reveal a gap (e.g. notification gateway not bound in production module), open a follow-up and reference this plan.

- [ ] **Step 15.4 — Final commit if any cleanup was needed**

```bash
git status
```

If the working tree has any unstaged cleanup from lint/smoke steps, commit it with a focused message. Otherwise, the feature is complete.

---

## Spec Coverage Map

| Spec requirement | Implemented in |
|---|---|
| FR-001 (upload PNG + zone) | Task 6 |
| FR-002 (reject non-PNG) | Task 6 (Step 6.4 validation) |
| FR-003 (zone non-negative + fits inside) | Task 6 (Step 6.4 `validatePngFitsZone`) |
| FR-004 (clear template) | Task 6/7 (DELETE handler + Task 4 `clear()`) |
| FR-005 (five nullable columns) | Task 1 |
| FR-006 (flyer-enabled iff all five non-null) | Task 1 (`hasFlyerTemplate`) |
| FR-007 (org-only generate endpoint) | Task 10 (with `AuthorizedOrganisationPlugin`) |
| FR-008 (reject when not validated) | Task 9 (Step 9.2 `ConflictException`) |
| FR-009 (reject when no logo) | Task 9 |
| FR-010 (reject when pack not flyer-enabled) | Task 9 |
| FR-011 (composition algorithm) | Task 8 |
| FR-012 (write to communication_support_url) | Task 9 |
| FR-013 (email partnership contacts) | Task 11 + Task 12 |
| FR-014 (Slack to organisers) | Task 11 + Task 12 |
| FR-015 (no special overwrite notification) | Task 12 (single notification variant fires for both first generation and regeneration) |
| FR-016 (digest flyer_items) | Task 13 |
| FR-017 (flyer count in hasItems) | Task 13 (Step 13.3) |
| User Story 1 acceptance scenarios | Tasks 6, 7 (PUT replaces old, DELETE clears all) |
| User Story 2 acceptance scenarios | Tasks 9, 10 (preconditions + happy path) |
| User Story 3 acceptance scenarios | Tasks 11, 12 |
| User Story 4 acceptance scenarios | Task 13 |
| Edge cases (logo larger/smaller than zone) | Task 8 |
| Edge case (zone out of bounds) | Task 6 |
| Edge case (non-PNG template) | Task 6 |
| Edge case (template URL unreachable) | Task 9 (HTTP failure surfaces as exception; communication_support_url not overwritten because storage upload only runs after composition succeeds) |
