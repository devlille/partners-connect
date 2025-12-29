# Quickstart: Synchronize Pack Options

**Feature**: 012-sync-pack-options  
**Date**: November 24, 2025

## Overview

This guide provides step-by-step instructions for implementing and validating the pack options synchronization feature. Follow these steps in order to ensure proper implementation following the project's constitution and testing standards.

---

## Prerequisites

Before starting implementation:

1. **Branch**: Verify you're on `012-sync-pack-options` branch
   ```bash
   git branch --show-current
   # Should output: 012-sync-pack-options
   ```

2. **Dependencies**: Ensure all dependencies are up to date
   ```bash
   cd server
   ./gradlew build --no-daemon
   ```

3. **Review Documentation**:
   - [spec.md](./spec.md) - Feature specification
   - [research.md](./research.md) - Implementation patterns
   - [data-model.md](./data-model.md) - Database schema
   - [contracts/sync_pack_options_contract.md](./contracts/sync_pack_options_contract.md) - API contract

---

## Step 1: Write Contract Tests (TDD Approach)

**CRITICAL**: Tests must be written BEFORE implementation (constitution requirement)

### Location
`server/application/src/test/kotlin/fr/devlille/partners/connect/sponsoring/SponsoringPackRoutesTest.kt`

### What to Modify

1. **Update existing test** that expects 409 error for already-attached options:
   ```kotlin
   // OLD: Expect 409 when option already attached
   @Test
   fun `POST packs options returns 409 if option is already attached to pack`()
   
   // NEW: Expect 201 (idempotent behavior)
   @Test
   fun `POST pack options is idempotent when option already attached`()
   ```

2. **Add new contract tests** (see contracts document for complete list):
   - Replace all options scenario
   - Partial overlap scenario  
   - Change requirement status scenario
   - Empty configuration scenario
   - Validation error scenarios (409, 403, 404)

### Example Test Structure

```kotlin
@Test
fun `POST pack options synchronizes by removing old and adding new options`() = testApplication {
    val orgId = UUID.randomUUID()
    val eventId = UUID.randomUUID()
    val eventSlug = "test-sync-pack-options-001"
    val packId = UUID.randomUUID()
    val optionA = UUID.randomUUID()
    val optionB = UUID.randomUUID()
    val optionC = UUID.randomUUID()
    val optionD = UUID.randomUUID()
    
    application {
        moduleMocked()
        insertMockedOrganisationEntity(orgId)
        insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        insertMockedSponsoringPack(packId, eventId)
        insertMockedSponsoringOption(optionA, eventId, "Logo")
        insertMockedSponsoringOption(optionB, eventId, "Booth")
        insertMockedSponsoringOption(optionC, eventId, "Talk")
        insertMockedSponsoringOption(optionD, eventId, "Article")
    }
    
    // First, attach options A and B
    client.post("/orgs/$orgId/events/$eventSlug/packs/$packId/options") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer valid")
        setBody(Json.encodeToString(AttachOptionsToPack(
            required = listOf(optionA.toString()),
            optional = listOf(optionB.toString())
        )))
    }
    
    // Then, sync to options C and D (should replace A and B)
    val response = client.post("/orgs/$orgId/events/$eventSlug/packs/$packId/options") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer valid")
        setBody(Json.encodeToString(AttachOptionsToPack(
            required = listOf(optionC.toString()),
            optional = listOf(optionD.toString())
        )))
    }
    
    assertEquals(HttpStatusCode.Created, response.status)
    
    // Verify pack now has only C and D (not A and B)
    val verifyResponse = client.get("/orgs/$orgId/events/$eventSlug/packs") {
        header(HttpHeaders.AcceptLanguage, "en")
        header(HttpHeaders.Authorization, "Bearer valid")
    }
    val body = verifyResponse.bodyAsText()
    assertTrue(body.contains("Talk"))     // Option C
    assertTrue(body.contains("Article"))  // Option D
    assertFalse(body.contains("Logo"))    // Option A removed
    assertFalse(body.contains("Booth"))   // Option B removed
}
```

### Run Tests (Should Fail Initially)

```bash
cd server
./gradlew test --no-daemon --tests "SponsoringPackRoutesTest"
```

**Expected**: Tests fail because implementation doesn't exist yet. This confirms TDD approach.

---

## Step 2: Implement Synchronization Logic

### Location
`server/application/src/main/kotlin/fr/devlille/partners/connect/sponsoring/application/OptionRepositoryExposed.kt`

### Method to Modify
`attachOptionsToPack(eventSlug: String, packId: UUID, options: AttachOptionsToPack)`

### Implementation Pattern

```kotlin
override fun attachOptionsToPack(eventSlug: String, packId: UUID, options: AttachOptionsToPack) = transaction {
    // 1. Existing validation (keep as-is)
    val event = EventEntity.findBySlug(eventSlug)
        ?: throw NotFoundException("Event with slug $eventSlug not found")
    val intersect = options.required.intersect(options.optional)
    if (intersect.isNotEmpty()) {
        throw ConflictException("options ${intersect.joinToString(",")} cannot be both required and optional")
    }
    val pack = SponsoringPackEntity.singlePackById(event.id.value, packId)
    
    // 2. Validate submitted options belong to event (existing logic - keep)
    val requiredOptions = SponsoringOptionEntity.find {
        (SponsoringOptionsTable.eventId eq event.id.value) and
        (SponsoringOptionsTable.id inList options.required.map(UUID::fromString))
    }.toList()
    
    val optionalOptions = SponsoringOptionEntity.find {
        (SponsoringOptionsTable.eventId eq event.id.value) and
        (SponsoringOptionsTable.id inList options.optional.map(UUID::fromString))
    }.toList()
    
    val existingOptions = (requiredOptions + optionalOptions).map { it.id.value.toString() }
    val allOptionIds = (options.required + options.optional).map(UUID::fromString).distinct()
    
    if (existingOptions.size != allOptionIds.size) {
        throw ForbiddenException("Some options do not belong to the event")
    }
    
    // 3. NEW: Delete options not in submitted lists
    val submittedOptionIds = allOptionIds.toSet()
    PackOptionsTable.deleteWhere {
        (pack eq packId) and (option notInList submittedOptionIds)
    }
    
    // 4. NEW: Get currently attached options to determine what to add vs update
    val currentlyAttached = PackOptionsTable
        .selectAll()
        .where { PackOptionsTable.pack eq packId }
        .map { 
            it[PackOptionsTable.option].value to it[PackOptionsTable.required] 
        }
        .toMap()
    
    // 5. NEW: Process required options (update if exists, insert if new)
    requiredOptions.forEach { option ->
        val optionId = option.id.value
        val currentlyRequired = currentlyAttached[optionId]
        
        when {
            currentlyRequired == null -> {
                // Option not attached - insert
                PackOptionsTable.insert {
                    it[this.pack] = pack.id
                    it[this.option] = option.id
                    it[this.required] = true
                }
            }
            currentlyRequired != true -> {
                // Option attached but status changed - update
                PackOptionsTable.update({ 
                    (pack eq packId) and (PackOptionsTable.option eq optionId) 
                }) {
                    it[required] = true
                }
            }
            // else: already attached as required - no action needed
        }
    }
    
    // 6. NEW: Process optional options (update if exists, insert if new)
    optionalOptions.forEach { option ->
        val optionId = option.id.value
        val currentlyRequired = currentlyAttached[optionId]
        
        when {
            currentlyRequired == null -> {
                // Option not attached - insert
                PackOptionsTable.insert {
                    it[this.pack] = pack.id
                    it[this.option] = option.id
                    it[this.required] = false
                }
            }
            currentlyRequired != false -> {
                // Option attached but status changed - update
                PackOptionsTable.update({ 
                    (pack eq packId) and (PackOptionsTable.option eq optionId) 
                }) {
                    it[required] = false
                }
            }
            // else: already attached as optional - no action needed
        }
    }
}
```

### Key Changes

1. **Remove**: Check for already-attached options (line ~220-230) - now idempotent
2. **Add**: Delete operation for removed options (step 3)
3. **Add**: Query current attachments (step 4)
4. **Modify**: Insert logic to handle updates (steps 5-6)

---

## Step 3: Run Tests Again

```bash
cd server
./gradlew test --no-daemon --tests "SponsoringPackRoutesTest"
```

**Expected**: All contract tests pass

**If tests fail**:
1. Check exception messages - constitution requires descriptive errors
2. Verify transaction boundaries - all operations in single `transaction {}`
3. Check validation order - validations before mutations
4. Review test assertions - ensure testing actual database state

---

## Step 4: Update OpenAPI Documentation

### Location
`server/application/src/main/resources/openapi/openapi.yaml`

### Changes Required

Find the POST operation at line ~2513:

```yaml
/orgs/{orgSlug}/events/{eventSlug}/packs/{packId}/options:
  post:
    summary: "Synchronize sponsoring pack options"  # CHANGED
    description: |                                   # UPDATED
      Synchronizes the complete set of options for a sponsoring pack.
      
      This endpoint accepts a complete configuration of required and optional options
      and ensures the pack's final state exactly matches the submitted configuration.
      
      Operations performed atomically:
      - Remove options not included in the request
      - Add new options from the request  
      - Update requirement status for existing options
      
      The operation is idempotent - submitting the same configuration multiple times
      produces the same result. Both required and optional lists can be empty.
```

### Validate OpenAPI Spec

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect
npm run validate
```

**Expected**: Validation passes with zero errors

---

## Step 5: Code Quality Checks

### Run ktlint

```bash
cd server
./gradlew ktlintCheck --no-daemon
```

**If violations found**:
```bash
./gradlew ktlintFormat --no-daemon
```

### Run detekt

```bash
./gradlew detekt --no-daemon
```

**Expected**: Zero violations (constitution requirement)

---

## Step 6: Full Test Suite

Run all tests to ensure no regressions:

```bash
cd server
./gradlew test --no-daemon
```

**Expected**: 
- All existing tests pass
- New tests pass
- Coverage ≥ 80% for modified code

---

## Step 7: Manual Validation (Optional)

### Start Local Server

```bash
cd server
./gradlew run --no-daemon
```

Server starts on http://localhost:8080

### Test via Bruno/Postman

1. **Authenticate** to get JWT token
2. **Create test pack** via POST `/orgs/{org}/events/{event}/packs`
3. **Create test options** via POST `/orgs/{org}/events/{event}/options`
4. **Sync options to pack**:
   ```
   POST /orgs/{org}/events/{event}/packs/{packId}/options
   
   {
     "required": ["option-uuid-1"],
     "optional": ["option-uuid-2"]
   }
   ```
5. **Verify synchronization** via GET `/orgs/{org}/events/{event}/packs`
6. **Re-sync with different options** to verify removal
7. **Test error cases** (duplicate IDs, invalid options, etc.)

---

## Step 8: Commit & Push

### Pre-commit Checklist

- ✅ All tests pass
- ✅ ktlint passes
- ✅ detekt passes  
- ✅ OpenAPI validation passes
- ✅ Coverage ≥ 80%
- ✅ No TODO comments without GitHub issues

### Commit Message Format

```
feat(sponsoring): implement pack options synchronization

- Modify attachOptionsToPack to sync instead of add-only
- Remove duplicate attachment check (now idempotent)
- Add delete operation for removed options
- Update requirement status for existing options
- Add contract tests for sync scenarios
- Update OpenAPI documentation

Closes #XXX
```

### Push to Remote

```bash
git add .
git commit -m "feat(sponsoring): implement pack options synchronization"
git push origin 012-sync-pack-options
```

---

## Validation Checklist

Use this checklist to verify implementation completeness:

### Functional Requirements

- [ ] FR-001: Accepts complete list of required and optional option IDs (no limit)
- [ ] FR-002: Removes existing options not in submitted lists
- [ ] FR-003: Adds new options from submitted lists
- [ ] FR-004: Updates requirement status for existing options that changed
- [ ] FR-005: Validates option IDs belong to same event as pack
- [ ] FR-006: Rejects duplicate option IDs (returns 409)
- [ ] FR-007: Rejects non-existent option IDs (returns 404)
- [ ] FR-008: Validates pack exists for event (returns 404)
- [ ] FR-009: Performs atomically in transaction with rollback
- [ ] FR-010: Maintains existing authorization (AuthorizedOrganisationPlugin)
- [ ] FR-011: Last-write-wins for concurrent modifications

### Success Criteria

- [ ] SC-001: Single API request updates complete configuration
- [ ] SC-002: Final state matches submitted configuration 100%
- [ ] SC-003: Completes in <500ms for 50 options
- [ ] SC-004: Validation errors identify specific problem option IDs
- [ ] SC-005: Zero partial states (atomic operations)

### Code Quality

- [ ] ktlint passes with zero violations
- [ ] detekt passes with zero violations
- [ ] Test coverage ≥ 80%
- [ ] KDoc documentation on modified methods
- [ ] No TODO comments without issues

### Testing

- [ ] Contract tests written BEFORE implementation
- [ ] All contract test scenarios implemented (9 minimum)
- [ ] Integration tests verify database state
- [ ] Tests use existing mock factories
- [ ] Tests follow existing patterns in SponsoringPackRoutesTest.kt

### Documentation

- [ ] OpenAPI summary updated
- [ ] OpenAPI description updated
- [ ] Operation behavior documented clearly
- [ ] Error responses documented (400, 401, 403, 404, 409, 500)
- [ ] Examples provided in contract docs

---

## Troubleshooting

### Tests Fail with "Option already attached"

**Cause**: Still using old add-only logic  
**Solution**: Ensure step 3 (delete removed options) is implemented before step 5 (insert new options)

### Tests Pass but Manual Testing Shows Old Options Remain

**Cause**: Delete query not including correct WHERE clause  
**Solution**: Verify `notInList` logic: `option notInList submittedOptionIds`

### Concurrent Tests Fail Intermittently

**Cause**: Transaction isolation  
**Solution**: This is expected with last-write-wins - verify final state matches one of the submitted configurations

### Performance Tests Timeout

**Cause**: Inefficient queries (N+1 problem)  
**Solution**: 
- Use `toList()` to fetch all options upfront
- Use bulk `deleteWhere` with `notInList`
- Use bulk `update` with `inList`

### OpenAPI Validation Fails

**Cause**: YAML syntax error or invalid schema reference  
**Solution**: 
- Check YAML indentation (2 spaces)
- Verify `$ref` paths match existing components
- Run `npm run validate` for detailed error messages

---

## Next Steps

After completing this quickstart:

1. **Create Pull Request** with branch `012-sync-pack-options`
2. **Request Code Review** from team members
3. **Address Review Feedback**
4. **Merge to main** once approved
5. **Monitor Production** for performance and errors after deployment

---

## Reference Documentation

- [Ktor Documentation](https://ktor.io/docs/)
- [Exposed ORM Documentation](https://github.com/JetBrains/Exposed/wiki)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [OpenAPI 3.1.0 Specification](https://swagger.io/specification/)
- [Project Constitution](../../.specify/memory/constitution.md)
