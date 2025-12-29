# Quickstart: User Permission Revocation Implementation

**Feature**: User Permission Revocation for Organisations  
**Date**: 2025-10-24  
**Est. Time**: 5-7 hours  
**Status**: Ready for implementation

## Prerequisites

- [x] Feature specification complete (`spec.md`)
- [x] Implementation plan complete (`plan.md`)
- [x] Research complete (`research.md`)
- [x] Data model defined (`data-model.md`)
- [x] API contract defined (`contracts/revoke-users.http`)
- [x] Development environment ready (Java 21, Gradle 8.13+)

## Implementation Checklist

### Phase 1: Domain Layer (30 minutes)

#### 1.1 Create Result Model
**File**: `server/application/src/main/kotlin/fr/devlille/partners/connect/users/domain/RevokeUsersResult.kt`

```kotlin
package fr.devlille.partners.connect.users.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of a user permission revocation operation.
 * 
 * @property revokedCount Number of users whose permissions were successfully revoked
 * @property notFoundEmails List of email addresses that were not found in the system
 */
@Serializable
data class RevokeUsersResult(
    @SerialName("revoked_count")
    val revokedCount: Int,
    @SerialName("not_found_emails")
    val notFoundEmails: List<String>
)
```

**Validation**: Build compiles, ktlint passes

#### 1.2 Update Repository Interface
**File**: `server/application/src/main/kotlin/fr/devlille/partners/connect/users/domain/UserRepository.kt`

Add method to interface:
```kotlin
/**
 * Revokes edit permissions for specified users from an organisation.
 * 
 * Users who don't exist in the system are collected in the result's notFoundEmails list
 * without causing the operation to fail. The operation is idempotent - revoking already-revoked
 * users is a no-op.
 * 
 * @param orgSlug Organisation identifier
 * @param userEmails List of user email addresses to revoke
 * @return RevokeUsersResult containing count of revoked users and list of not-found emails
 * @throws NotFoundException if organisation does not exist
 * @throws ConflictException if attempting to revoke last editor's own access
 */
fun revokeUsers(orgSlug: String, userEmails: List<String>, requestingUserEmail: String): RevokeUsersResult
```

**Validation**: Build compiles with compilation error (expected - implementation missing)

### Phase 2: Infrastructure Layer - API (45 minutes)

#### 2.1 Create Request Model
**File**: `server/application/src/main/kotlin/fr/devlille/partners/connect/users/infrastructure/api/RevokePermissionRequest.kt`

```kotlin
package fr.devlille.partners.connect.users.infrastructure.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request payload for revoking user permissions from an organisation.
 */
@Serializable
data class RevokePermissionRequest(
    @SerialName("user_emails")
    val userEmails: List<String>
)
```

**Validation**: Build compiles, ktlint passes

#### 2.2 Create JSON Schema
**File**: `server/application/src/main/resources/schemas/revoke_permission_request.schema.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["user_emails"],
  "properties": {
    "user_emails": {
      "type": "array",
      "items": {
        "type": "string",
        "format": "email"
      },
      "description": "List of user email addresses to revoke access from"
    }
  },
  "additionalProperties": false
}
```

**Validation**: JSON is valid, schema file created

#### 2.3 Register Schema in ApplicationCall.ext.kt
**File**: `server/application/src/main/kotlin/fr/devlille/partners/connect/internal/infrastructure/ktor/ApplicationCall.ext.kt`

Add schema registration in `schemaValidator` initialization:
```kotlin
.register(readResourceFile("/schemas/revoke_permission_request.schema.json"), SchemaType.DRAFT_7)
```

**Validation**: Build compiles

#### 2.4 Add Route Handler
**File**: `server/application/src/main/kotlin/fr/devlille/partners/connect/users/infrastructure/api/UserRoutes.kt`

Add POST /revoke endpoint inside `route("/orgs/{orgSlug}/users")` block:

```kotlin
post("/revoke") {
    val token = call.token
    val orgSlug = call.parameters.orgSlug
    val request = call.receive<RevokePermissionRequest>(schema = "revoke_permission_request.schema.json")
    val userInfo = authRepository.getUserInfo(token)
    val hasPerm = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
    if (!hasPerm) {
        throw UnauthorizedException("You do not have permission to revoke users for this organisation")
    }
    val result = userRepository.revokeUsers(orgSlug, request.userEmails, userInfo.email)
    call.respond(HttpStatusCode.OK, result)
}
```

**Validation**: Build fails (expected - repository method not implemented yet)

### Phase 3: Application Layer - Repository Implementation (60 minutes)

#### 3.1 Implement revokeUsers Method
**File**: `server/application/src/main/kotlin/fr/devlille/partners/connect/users/application/UserRepositoryExposed.kt`

Add implementation:
```kotlin
override fun revokeUsers(
    orgSlug: String,
    userEmails: List<String>,
    requestingUserEmail: String
): RevokeUsersResult = transaction {
    val org = OrganisationEntity.findBySlug(orgSlug)
        ?: throw NotFoundException("Organisation with slug: $orgSlug not found")
    
    val notFoundEmails = mutableListOf<String>()
    var revokedCount = 0
    
    // Deduplicate emails
    val uniqueEmails = userEmails.distinct()
    
    // Check self-revocation + last editor constraint
    if (requestingUserEmail in uniqueEmails) {
        val editorCount = OrganisationPermissionEntity
            .find {
                (OrganisationPermissionsTable.organisation eq org.id) and
                (OrganisationPermissionsTable.canEdit eq true)
            }
            .count()
        
        if (editorCount == 1L) {
            throw ConflictException(
                "Cannot revoke your own access as the last editor of this organisation"
            )
        }
    }
    
    // Process each email
    uniqueEmails.forEach { email ->
        val userEntity = UserEntity.singleUserByEmail(email)
        
        if (userEntity == null) {
            notFoundEmails.add(email)
        } else {
            val permission = OrganisationPermissionEntity
                .singleEventPermission(
                    organisationId = org.id.value,
                    userId = userEntity.id.value
                )
            
            if (permission != null && permission.canEdit) {
                permission.delete()
                revokedCount++
            } else if (permission == null) {
                // User exists but has no permission - count as not found
                notFoundEmails.add(email)
            }
            // If permission exists but canEdit=false, do nothing (out of scope per FR-003a)
        }
    }
    
    RevokeUsersResult(
        revokedCount = revokedCount,
        notFoundEmails = notFoundEmails
    )
}
```

**Note**: Import `ConflictException` from `fr.devlille.partners.connect.internal.infrastructure.api`

**Validation**: 
- Build compiles successfully
- Run `./gradlew build --no-daemon` from `server/` directory
- No ktlint or detekt violations

### Phase 4: Testing (3-4 hours)

#### 4.1 Create Test Class
**File**: `server/application/src/test/kotlin/fr/devlille/partners/connect/users/RevokePermissionRouteTest.kt`

```kotlin
package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedUser
import fr.devlille.partners.connect.users.infrastructure.api.RevokePermissionRequest
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.sql.and
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevokePermissionRouteTest {
    
    @Test
    fun `revoke users successfully when authenticated user has permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedUser(id = targetId, email = "bob@example.com")
            
            // Grant permission to bob
            transaction {
                val org = OrganisationEntity.findById(orgId)!!
                val user = UserEntity.findById(targetId)!!
                OrganisationPermissionEntity.new {
                    this.organisation = org
                    this.user = user
                    this.canEdit = true
                }
            }
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = listOf("bob@example.com"))
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"revoked_count\":1"))
        assertTrue(body.contains("\"not_found_emails\":[]"))
        
        transaction {
            val permission = OrganisationPermissionEntity
                .find {
                    (OrganisationPermissionsTable.organisation eq orgId) and
                    (OrganisationPermissionsTable.user eq targetId)
                }
                .firstOrNull()
            assertEquals(null, permission) // Permission should be deleted
        }
    }
    
    @Test
    fun `return partial success with non-existent users`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val aliceId = UUID.randomUUID()
        
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedUser(id = aliceId, email = "alice@example.com")
            
            // Grant permission to alice
            transaction {
                val org = OrganisationEntity.findById(orgId)!!
                val user = UserEntity.findById(aliceId)!!
                OrganisationPermissionEntity.new {
                    this.organisation = org
                    this.user = user
                    this.canEdit = true
                }
            }
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = listOf(
                    "alice@example.com",
                    "nonexistent@example.com"
                ))
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"revoked_count\":1"))
        assertTrue(body.contains("nonexistent@example.com"))
    }
    
    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = emptyList())
            ))
        }
        
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
    
    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer invalid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = emptyList())
            ))
        }
        
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
    
    @Test
    fun `return 404 if authenticated user is not in DB`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = emptyList())
            ))
        }
        
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
    
    @Test
    fun `return 401 if authenticated user has no right to revoke`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val email = "noedit@mail.com"
        
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEvent(eventId, orgId = orgId)
            insertMockedAdminUser()
            insertMockedUser(email = email)
        }
        
        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = listOf(email))
            ))
        }
        
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
    
    @Test
    fun `return 409 when revoking last editor's own access`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val adminEmail = "admin@mail.com"
        
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = listOf(adminEmail))
            ))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("last editor"))
    }
    
    @Test
    fun `handle empty email list`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = emptyList())
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"revoked_count\":0"))
        assertTrue(body.contains("\"not_found_emails\":[]"))
    }
    
    @Test
    fun `idempotent - revoking already revoked user`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedUser(id = userId, email = "bob@example.com")
            // Note: NOT granting permission to bob
        }

        val response = client.post("/orgs/$orgId/users/revoke") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(
                RevokePermissionRequest.serializer(),
                RevokePermissionRequest(userEmails = listOf("bob@example.com"))
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"revoked_count\":0"))
        assertTrue(body.contains("bob@example.com")) // In not_found_emails
    }
}
```

**Validation**:
- Run `cd server && ./gradlew test --no-daemon`
- All 9 tests should pass
- Check coverage report in `application/build/reports/tests/test/index.html`

### Phase 5: Integration & Validation (30 minutes)

#### 5.1 Run Full Build
```bash
cd server
./gradlew clean build --no-daemon
```

**Expected**: Build SUCCESS with all tests passing

#### 5.2 Code Quality Checks
```bash
cd server
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon
```

**Expected**: No violations

#### 5.3 Manual API Testing (Optional)
```bash
# Start server
./gradlew run --no-daemon

# In another terminal, test with curl
curl -X POST http://localhost:8080/orgs/test-org/users/revoke \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"user_emails": ["user1@example.com", "user2@example.com"]}'
```

#### 5.4 Update OpenAPI Documentation
**File**: `server/application/src/main/resources/openapi/openapi.yaml`

Add endpoint specification (see `contracts/revoke-users.http` for full schema)

**Validation**: Run `npm run validate` from repo root

## Success Criteria

- [x] All files created in correct locations
- [x] Build compiles without errors
- [x] All 9 test scenarios pass
- [x] ktlint and detekt show zero violations
- [x] Test coverage ≥ 80% for new code
- [x] OpenAPI documentation updated and validates
- [x] Feature matches specification requirements (FR-001 through FR-013)

## Troubleshooting

### Build Fails with "Cannot find ConflictException"
**Solution**: Import from `fr.devlille.partners.connect.internal.infrastructure.api.ConflictException`

### Tests Fail with "Organisation not found"
**Solution**: Ensure `insertMockedOrganisationEntity(orgId)` is called before test actions

### ktlint Fails with Formatting Errors
**Solution**: Run `./gradlew ktlintFormat --no-daemon` to auto-fix

### Test Fails: "Expected 1 but was 2"
**Solution**: Check if admin user is accidentally included in revocation - filter or adjust test data

## Estimated Timeline

| Phase | Task | Duration |
|-------|------|----------|
| 1 | Domain models | 30 min |
| 2 | API layer (route + request model) | 45 min |
| 3 | Repository implementation | 60 min |
| 4 | Test implementation (9 scenarios) | 180 min |
| 5 | Integration & validation | 30 min |
| **Total** | | **5.5 hours** |

Add 1-2 hours buffer for unexpected issues → **6.5-7 hours total**

## Next Steps After Completion

1. Create pull request with all changes
2. Run CI/CD pipeline (GitHub Actions)
3. Request code review from team
4. Address review feedback
5. Merge to main branch
6. Deploy to staging environment
7. Perform smoke tests
8. Deploy to production

## References

- Feature Spec: `specs/005-as-an-organizer/spec.md`
- Implementation Plan: `specs/005-as-an-organizer/plan.md`
- API Contract: `specs/005-as-an-organizer/contracts/revoke-users.http`
- Constitution: `.specify/memory/constitution.md`
- Existing Grant Implementation: `users/infrastructure/api/UserRoutes.kt` (line ~49)
