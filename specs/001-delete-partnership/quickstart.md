# Quickstart Guide: Delete Unvalidated Partnership

**Feature**: Delete Unvalidated Partnership  
**Date**: December 6, 2025  
**Phase**: 1 - Design & Contracts

## Overview

This guide provides step-by-step instructions for implementing and testing the delete unvalidated partnership feature. Follow these steps in order to ensure compliance with the project's constitution and quality standards.

## Prerequisites

- Java 21 (Amazon Corretto) installed
- Gradle 8.13+ (wrapper included in project)
- PostgreSQL running locally or via Docker (for manual testing)
- Node.js 18+ and npm (for OpenAPI validation)
- Git checkout of branch `001-delete-partnership`

## Development Workflow

### Phase 1: Write Contract Tests (TDD)

**Goal**: Write failing tests that define the API contract before implementation exists.

1. **Navigate to test directory**:
   ```bash
   cd server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/contract
   ```

2. **Create or update `PartnershipContractTest.kt`**:
   ```kotlin
   @Test
   fun `DELETE partnership returns 204 when successful`() {
       withTestDatabase {
           // Setup: Create unvalidated partnership
           val event = insertMockedEvent()
           val company = insertMockedCompany()
           val partnership = insertMockedPartnership(
               eventId = event.id.value,
               companyId = company.id.value,
               validatedAt = null,
               declinedAt = null
           )
           
           // When: DELETE request
           testClient.delete("/orgs/${event.orgSlug}/events/${event.slug}/partnerships/${partnership.id}") {
               bearerAuth(generateValidToken(withEditPermission = true))
           }.apply {
               // Then: 204 No Content
               assertEquals(HttpStatusCode.NoContent, status)
           }
       }
   }
   
   @Test
   fun `DELETE finalized partnership returns 409 Conflict`() {
       withTestDatabase {
           val event = insertMockedEvent()
           val company = insertMockedCompany()
           val partnership = insertMockedPartnership(
               eventId = event.id.value,
               companyId = company.id.value,
               validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
               declinedAt = null
           )
           
           testClient.delete("/orgs/${event.orgSlug}/events/${event.slug}/partnerships/${partnership.id}") {
               bearerAuth(generateValidToken(withEditPermission = true))
           }.apply {
               assertEquals(HttpStatusCode.Conflict, status)
           }
       }
   }
   ```

3. **Run tests (should fail)**:
   ```bash
   cd /Users/gpaligot/Documents/workspace/partners-connect/server
   ./gradlew test --tests "PartnershipContractTest" --no-daemon
   ```
   
   **Expected**: Tests fail because DELETE endpoint doesn't exist yet ✅

### Phase 2: Update Domain Interface

**Goal**: Add delete method signature to repository interface.

1. **Open file**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipRepository.kt`

2. **Add method to interface**:
   ```kotlin
   /**
    * Deletes an unvalidated partnership.
    *
    * Only partnerships where both validatedAt and declinedAt are null can be deleted.
    * Performs a hard delete with no audit trail.
    *
    * @param partnershipId UUID of the partnership to delete
    * @throws fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
    *   if partnership not found
    * @throws fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
    *   if partnership is finalized (validatedAt or declinedAt is set)
    */
   fun delete(partnershipId: UUID)
   ```

3. **Save file** (no compilation yet - interface change only)

### Phase 3: Implement Repository Method

**Goal**: Implement delete logic with state validation.

1. **Open file**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/PartnershipRepositoryExposed.kt`

2. **Add implementation**:
   ```kotlin
   override fun delete(partnershipId: UUID) {
       val partnership = PartnershipEntity.findById(partnershipId)
           ?: throw NotFoundException("Partnership not found")
       
       // Validate partnership is in unvalidated state
       if (partnership.validatedAt != null || partnership.declinedAt != null) {
           throw ConflictException("Cannot delete finalized partnership")
       }
       
       // Hard delete
       partnership.delete()
   }
   ```

3. **Add imports**:
   ```kotlin
   import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
   import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
   ```

### Phase 4: Add DELETE Route

**Goal**: Expose delete functionality via REST API.

1. **Open file**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipRoutes.kt`

2. **Find `orgsPartnershipRoutes()` function** (around line 100-150)

3. **Add DELETE endpoint** inside the route that already has `install(AuthorizedOrganisationPlugin)`:
   ```kotlin
   private fun Route.orgsPartnershipRoutes() {
       val partnershipRepository by inject<PartnershipRepository>()
       
       route("/orgs/{orgSlug}/events/{eventSlug}/partnerships") {
           install(AuthorizedOrganisationPlugin)  // Already installed
           
           // ... existing endpoints (GET, PUT, etc.) ...
           
           delete("/{partnershipId}") {
               val partnershipId = call.parameters.partnershipId
               partnershipRepository.delete(partnershipId)
               call.respond(HttpStatusCode.NoContent)
           }
       }
   }
   ```

4. **Add import** (if not already present):
   ```kotlin
   import io.ktor.http.HttpStatusCode
   ```

### Phase 5: Update OpenAPI Documentation

**Goal**: Document DELETE endpoint in OpenAPI specification.

1. **Open file**: `server/application/src/main/resources/openapi/openapi.yaml`

2. **Find the path** `/orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}`

3. **Add DELETE operation** (copy from `specs/001-delete-partnership/contracts/delete_partnership.yaml`):
   ```yaml
   /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId}:
     # ... existing GET, PUT operations ...
     
     delete:
       operationId: deletePartnership
       summary: Delete an unvalidated partnership
       description: |
         Deletes a partnership that has not been finalized (validated or declined).
         Only partnerships where both validatedAt and declinedAt are null can be deleted.
         Requires edit permission on the organization that owns the event.
       tags:
         - Partnerships
       security:
         - bearerAuth: []
       parameters:
         - $ref: '#/components/parameters/orgSlug'
         - $ref: '#/components/parameters/eventSlug'
         - name: partnershipId
           in: path
           required: true
           schema:
             type: string
             format: uuid
       responses:
         '204':
           description: Partnership successfully deleted
         '401':
           $ref: '#/components/responses/Unauthorized'
         '404':
           $ref: '#/components/responses/NotFound'
         '409':
           $ref: '#/components/responses/Conflict'
   ```

### Phase 6: Validate Implementation

**Goal**: Ensure code quality and OpenAPI compliance.

1. **Run ktlint and detekt**:
   ```bash
   cd /Users/gpaligot/Documents/workspace/partners-connect/server
   ./gradlew ktlintCheck detekt --no-daemon
   ```
   
   **Expected**: Zero violations ✅

2. **Fix formatting if needed**:
   ```bash
   ./gradlew ktlintFormat --no-daemon
   ```

3. **Validate OpenAPI specification**:
   ```bash
   cd /Users/gpaligot/Documents/workspace/partners-connect
   npm run validate
   ```
   
   **Expected**: Zero errors (warnings acceptable) ✅

### Phase 7: Run Tests

**Goal**: Verify all tests pass with implementation.

1. **Run contract tests**:
   ```bash
   cd /Users/gpaligot/Documents/workspace/partners-connect/server
   ./gradlew test --tests "*PartnershipContractTest*" --no-daemon
   ```
   
   **Expected**: All contract tests pass ✅

2. **Run all tests**:
   ```bash
   ./gradlew test --no-daemon
   ```
   
   **Expected**: All 95+ tests pass ✅

3. **Check coverage** (optional):
   ```bash
   ./gradlew jacocoTestReport --no-daemon
   ```
   
   **Target**: >80% coverage for new delete functionality ✅

### Phase 8: Manual Testing (Optional)

**Goal**: Manually verify delete operation via API.

1. **Start server**:
   ```bash
   cd /Users/gpaligot/Documents/workspace/partners-connect/server
   ./gradlew run --no-daemon
   ```

2. **Create test partnership** (use Bruno or curl):
   ```bash
   curl -X POST http://localhost:8080/events/test-event/partnerships \
     -H "Content-Type: application/json" \
     -d '{
       "companyId": "uuid-here",
       "contactName": "Test User",
       "contactRole": "Manager",
       "language": "en"
     }'
   ```

3. **Delete partnership**:
   ```bash
   curl -X DELETE http://localhost:8080/orgs/test-org/events/test-event/partnerships/{partnership-id} \
     -H "Authorization: Bearer {jwt-token}" \
     -v
   ```
   
   **Expected**: HTTP 204 No Content ✅

4. **Verify deletion** (try to get deleted partnership):
   ```bash
   curl -X GET http://localhost:8080/events/test-event/partnerships/{partnership-id} \
     -v
   ```
   
   **Expected**: HTTP 404 Not Found ✅

## Verification Checklist

Before committing, verify:

- [ ] Contract tests written and passing
- [ ] Integration tests cover all scenarios (success, 401, 404, 409)
- [ ] Repository interface updated with KDoc
- [ ] Repository implementation includes state validation
- [ ] DELETE route added under `AuthorizedOrganisationPlugin`
- [ ] OpenAPI documentation complete and valid (`npm run validate`)
- [ ] ktlint + detekt pass with zero violations
- [ ] All tests pass (`./gradlew test`)
- [ ] Code coverage >80% for new functionality
- [ ] No try-catch blocks in route handler (StatusPages handles exceptions)
- [ ] No manual permission checks (AuthorizedOrganisationPlugin handles it)

## Common Issues & Solutions

### Issue: Tests fail with "Partnership not found"
**Solution**: Ensure mock factory creates partnership with correct foreign keys (eventId, companyId)

### Issue: Tests fail with "Unauthorized"
**Solution**: Generate JWT token with `canEdit=true` permission for test organization

### Issue: OpenAPI validation errors
**Solution**: Check that all `$ref` references resolve correctly, ensure operationId is unique

### Issue: ktlint violations
**Solution**: Run `./gradlew ktlintFormat --no-daemon` to auto-fix formatting

### Issue: Detekt violations
**Solution**: Review detekt output, add suppression annotations only if justified (document reason)

## Next Steps

After completing this quickstart:

1. **Commit changes** to branch `001-delete-partnership`
2. **Push branch** and create pull request
3. **CI/CD validation** will run automatically (should pass)
4. **Code review** from team
5. **Merge** after approval

## References

- **Spec**: `specs/001-delete-partnership/spec.md`
- **Research**: `specs/001-delete-partnership/research.md`
- **Data Model**: `specs/001-delete-partnership/data-model.md`
- **Contract**: `specs/001-delete-partnership/contracts/delete_partnership.yaml`
- **Constitution**: `.specify/memory/constitution.md`
- **Copilot Instructions**: `.github/copilot-instructions.md`
