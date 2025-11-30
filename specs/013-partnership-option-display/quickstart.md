# Quickstart: Partnership Option Display Enhancement

**Feature**: Display Partnership-Specific Options  
**Spec ID**: 013-partnership-option-display  
**Estimated Effort**: 5-8 hours

---

## Prerequisites

### Development Environment

**Required Tools**:
- **Java 21** (Amazon Corretto recommended)
- **Gradle 8.13+** (wrapper included in repo)
- **PostgreSQL** (for full integration testing) OR H2 (in-memory for unit tests)
- **Node.js 18+** and **npm** (for OpenAPI validation)
- **Git** (for version control)

**Recommended IDE**:
- **IntelliJ IDEA** (with Kotlin plugin)
- **VS Code** (with Kotlin Language Server)

### Codebase Setup

```bash
# Clone repository (if not already)
git clone <repository-url>
cd partners-connect

# Checkout feature branch
git checkout 013-partnership-option-display

# Install backend dependencies (automatic via Gradle wrapper)
cd server
./gradlew build --no-daemon  # Initial build to download dependencies

# Install frontend dependencies (for OpenAPI validation)
npm install
```

---

## Development Workflow

### Phase 1: Domain Model Changes

**Files to Create/Modify**:
1. **Create**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipOption.kt`
2. **Modify**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipDetail.kt`
3. **Modify**: `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/domain/PartnershipPack.kt`

**Commands**:
```bash
cd server

# Run tests continuously during development
./gradlew test --continuous --no-daemon

# Check code style
./gradlew ktlintCheck --no-daemon
./gradlew detekt --no-daemon
```

**Key Steps**:
- Define `PartnershipOption` sealed class with 4 subtypes (Text, Quantitative, Number, Selectable)
- Add `currency` field to `PartnershipDetail`
- Change `PartnershipPack.options` to two lists: `requiredOptions` and `optionalOptions`, add `totalPrice`

**Validation**:
```bash
# Ensure compilation succeeds
./gradlew compileKotlin --no-daemon
```

---

### Phase 2: Repository Implementation

**Files to Modify**:
1. `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/db/PartnershipRepositoryExposed.kt`

**File to Create**:
1. `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/application/mappers/PartnershipOptionEntity.ext.kt`

**Key Steps**:
- Implement `PartnershipOptionEntity.toDomain()` extension function
- Update `PartnershipRepositoryExposed.getByIdDetailed()` to:
  - Fetch partnership-specific options (required + selected optional)
  - Calculate pack `totalPrice` for each pack
  - Map options to domain models with complete descriptions
  - Validate translations exist for partnership language

**Critical Logic**:
```kotlin
// Complete description formatting (parentheses separator)
val completeDesc = when (optionType) {
    TEXT -> description  // No suffix
    QUANTITATIVE -> "$description ($quantity ${descriptor.name})"
    NUMBER -> "$description ($fixedQuantity ${descriptor.name})"
    SELECTABLE -> "$description (${selectedValue.value})"
}

// Total price calculation (per pack)
val packTotalPrice = pack.basePrice + pack.optionalOptions.sumOf { it.totalPrice }
```

**Validation**:
```bash
# Run repository tests
./gradlew test --tests "*PartnershipRepository*" --no-daemon
```

---

### Phase 3: Update Billing/Agreement Endpoints

**Files to Modify**:
1. `server/application/src/main/kotlin/fr/devlille/partners/connect/billing/infrastructure/api/BillingRoutes.kt`
2. `server/application/src/main/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipAgreementRepositoryExposed.kt`

**Key Changes**:
- Replace `partnershipBillingRepository.computePricing()` calls with `partnershipRepository.getByIdDetailed()`
- Extract pricing data from enhanced partnership detail:
  ```kotlin
  val partnershipDetail = partnershipRepository.getByIdDetailed(eventSlug, partnershipId)
  val validatedPack = partnershipDetail.validatedPack
      ?: throw NotFoundException("No validated pack found")
  val pricing = PartnershipPricing(
      packBasePrice = validatedPack.basePrice,
      totalPrice = validatedPack.totalPrice,
      options = validatedPack.optionalOptions,
      currency = partnershipDetail.currency,
  )
  ```

**Validation**:
```bash
# Run billing/agreement tests
./gradlew test --tests "*Billing*" --tests "*Agreement*" --no-daemon
```

---

### Phase 4: JSON Schema Updates

**Files to Create**:
1. `server/application/src/main/resources/schemas/partnership_option.schema.json`
2. `server/application/src/main/resources/schemas/partnership_pack.schema.json` (update)
3. `server/application/src/main/resources/schemas/partnership_detail.schema.json` (update)

**Files to Modify**:
1. `server/application/src/main/resources/openapi.yaml`

**Key Steps**:
- Copy schema files from `specs/013-partnership-option-display/contracts/` to `server/application/src/main/resources/schemas/`
- Update `openapi.yaml` components section with new schemas
- Update endpoint response definitions to reference enhanced schemas

**Validation Commands**:
```bash
cd server

# Validate OpenAPI spec
npm run validate

# Bundle OpenAPI spec (for frontend generation)
npm run bundle

# Check for schema errors
./gradlew test --tests "*Contract*" --no-daemon
```

**Expected Output**:
```
✓ OpenAPI spec is valid
✓ No circular references
✓ All $ref pointers resolve
✓ Schema components registered
```

---

### Phase 5: Update Tests

**Files to Modify**:
1. `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipDetailedGetRouteTest.kt`
2. `server/application/src/test/kotlin/fr/devlille/partners/connect/billing/infrastructure/api/BillingRoutesTest.kt`
3. `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/AgreementRoutesTest.kt`

**Key Updates**:
- Add assertions for new fields (`currency` in PartnershipDetail, `totalPrice` in PartnershipPack)
- Update pack option assertions (now `PartnershipOption` not `SponsoringOption`)
- Verify complete description formatting
- Update billing/agreement test mocks to expect partnership detail calls

**Mock Factory Pattern**:
```kotlin
// Add to existing mock factories
fun insertMockedPartnershipWithOptions(
    language: String = "en",
    requiredOptions: List<SponsoringOptionEntity> = emptyList(),
    selectedOptions: List<Pair<SponsoringOptionEntity, Int>> = emptyList(),
): PartnershipEntity {
    val partnership = insertMockedPartnership(language = language)
    requiredOptions.forEach { option ->
        PartnershipOptionEntity.new {
            this.partnership = partnership
            this.option = option
            this.selectedQuantity = null  // Required options
        }
    }
    selectedOptions.forEach { (option, qty) ->
        PartnershipOptionEntity.new {
            this.partnership = partnership
            this.option = option
            this.selectedQuantity = qty
        }
    }
    return partnership
}
```

**Run All Tests**:
```bash
cd server

# Full test suite
./gradlew test --no-daemon

# Coverage report (optional)
./gradlew jacocoTestReport --no-daemon
# View report at: application/build/reports/jacoco/test/html/index.html
```

---

## Testing Strategy

### Update Existing Tests (No New Test Files)

**Test Files to Modify**:
1. `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/PartnershipDetailedGetRouteTest.kt`
2. `server/application/src/test/kotlin/fr/devlille/partners/connect/billing/infrastructure/api/BillingRoutesTest.kt`
3. `server/application/src/test/kotlin/fr/devlille/partners/connect/partnership/infrastructure/api/AgreementRoutesTest.kt`

**Changes Required**:

#### 1. PartnershipDetailedGetRouteTest Updates

**Add New Assertions** (to existing test methods):
```kotlin
@Test
fun `GET partnership detail returns complete information`() {
    // ... existing test setup ...
    
    // When
    val response = client.get("/events/devlille-2025/partnerships/${partnership.id}")
    
    // Then
    assertEquals(HttpStatusCode.OK, response.status)
    val body = response.body<DetailedPartnershipResponse>()
    
    // EXISTING ASSERTIONS (keep these)
    // ... company, event, organisation assertions ...
    
    // NEW ASSERTIONS - Currency field
    assertEquals("EUR", body.partnership.currency)
    
    // NEW ASSERTIONS - Pack structure changes
    val validatedPack = body.partnership.validatedPack
    assertNotNull(validatedPack)
    
    // NEW ASSERTIONS - Separate option lists
    assertTrue(validatedPack.requiredOptions.isNotEmpty() || validatedPack.optionalOptions.isNotEmpty())
    
    // NEW ASSERTIONS - Total price calculation
    assertTrue(validatedPack.totalPrice >= validatedPack.basePrice)
    
    // NEW ASSERTIONS - Option properties
    validatedPack.requiredOptions.forEach { option ->
        assertNotNull(option.id)
        assertNotNull(option.name)
        assertNotNull(option.description)
        assertNotNull(option.completeDescription)
        assertTrue(option.price >= 0)
        assertTrue(option.quantity >= 0)
        assertTrue(option.totalPrice >= 0)
    }
    
    validatedPack.optionalOptions.forEach { option ->
        assertNotNull(option.id)
        assertNotNull(option.name)
        assertNotNull(option.description)
        assertNotNull(option.completeDescription)
        assertTrue(option.price >= 0)
        assertTrue(option.quantity >= 0)
        assertTrue(option.totalPrice >= 0)
        
        // Verify complete description format (has parentheses)
        assertTrue(
            option.completeDescription.contains("(") && option.completeDescription.contains(")"),
            "Complete description should have parentheses format for typed options"
        )
    }
}
```

#### 2. BillingRoutesTest Updates

**Update Existing Invoice/Quote Test Assertions**:
```kotlin
@Test
fun `POST invoice creation uses partnership detail pricing`() {
    // ... existing test setup ...
    
    // When
    val response = client.post("/orgs/$orgSlug/events/$eventSlug/partnerships/${partnership.id}/invoice")
    
    // Then
    assertEquals(HttpStatusCode.OK, response.status)
    
    // UPDATED ASSERTION - Verify partnership detail was called (not billing repository)
    verify(exactly = 1) { partnershipRepository.getByIdDetailed(eventSlug, partnership.id) }
    verify(exactly = 0) { partnershipBillingRepository.computePricing(any(), any()) }
    
    // NEW ASSERTION - Verify validated pack pricing used
    val capturedPricing = slot<PartnershipPricing>()
    verify { billingRepository.createInvoice(capture(capturedPricing)) }
    
    assertEquals(validatedPack.basePrice, capturedPricing.captured.packBasePrice)
    assertEquals(validatedPack.totalPrice, capturedPricing.captured.totalPrice)
    assertEquals("EUR", capturedPricing.captured.currency)
}
```

#### 3. AgreementRoutesTest Updates

**Update Existing Agreement Generation Test**:
```kotlin
@Test
fun `POST agreement generation includes complete option descriptions`() {
    // ... existing test setup ...
    
    // When
    val response = client.post("/orgs/$orgSlug/events/$eventSlug/partnerships/${partnership.id}/agreement")
    
    // Then
    assertEquals(HttpStatusCode.OK, response.status)
    
    // NEW ASSERTION - Verify partnership detail was called
    verify(exactly = 1) { partnershipRepository.getByIdDetailed(eventSlug, partnership.id) }
    
    // NEW ASSERTION - Verify complete descriptions passed to agreement generation
    val capturedOptions = slot<List<PartnershipOption>>()
    verify { agreementRepository.generate(capture(capturedOptions), any(), any()) }
    
    capturedOptions.captured.forEach { option ->
        assertNotNull(option.completeDescription)
        // Verify formatted descriptions (not just base description)
        if (option is QuantitativePartnershipOption || option is NumberPartnershipOption) {
            assertTrue(option.completeDescription.contains("("))
        }
    }
}
```

### Mock Data Updates

**Update Existing Mock Factories** (in test utilities):
```kotlin
// Update existing factory to include pack options data
fun insertMockedPartnership(
    language: String = "en",
    withOptions: Boolean = true,
): PartnershipEntity {
    val partnership = PartnershipEntity.new {
        // ... existing fields ...
        this.language = language
    }
    
    // NEW: Add partnership options if requested
    if (withOptions) {
        val pack = partnership.validatedPack
        pack?.let {
            // Add required option
            val requiredOption = insertMockedSponsoringOption(optionType = OptionType.TEXT, required = true)
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.option = requiredOption
                this.selectedQuantity = null
            }
            
            // Add optional option
            val optionalOption = insertMockedSponsoringOption(optionType = OptionType.TYPED_QUANTITATIVE, required = false)
            PartnershipOptionEntity.new {
                this.partnership = partnership
                this.option = optionalOption
                this.selectedQuantity = 5
            }
        }
    }
    
    return partnership
}
```

### Expected Test Changes Summary

**DO NOT**:
- Create new test files
- Add new test methods (unless replacing deprecated ones)
- Change test structure significantly

**DO**:
- Add assertions to existing tests for new fields (`currency`, `totalPrice`, `requiredOptions`, `optionalOptions`)
- Update mocks to populate partnership options data
- Verify complete description formatting in existing tests
- Update billing/agreement tests to expect `partnershipRepository.getByIdDetailed()` calls
- Keep existing assertions for backward-compatible fields

### Validation Commands

```bash
cd server

# Run updated tests
./gradlew test --tests "*PartnershipDetailedGetRouteTest" --no-daemon
./gradlew test --tests "*BillingRoutesTest" --no-daemon
./gradlew test --tests "*AgreementRoutesTest" --no-daemon

# Full test suite (should still pass)
./gradlew test --no-daemon
```

---

## Quality Gates (Non-Negotiable)

### Pre-Commit Checks

```bash
cd server

# MUST pass before committing
./gradlew ktlintCheck detekt test build --no-daemon
```

**Expected Results**:
- ✅ ktlint: 0 violations
- ✅ detekt: 0 violations  
- ✅ tests: All passing (95+ tests)
- ✅ build: Successful compilation

### OpenAPI Validation

```bash
cd server

# MUST pass before merging
npm run validate
npm run bundle
```

**Expected Results**:
- ✅ OpenAPI spec valid
- ✅ All schemas resolve
- ✅ No circular references

### Integration Validation

```bash
# Terminal 1: Start backend
cd server
./gradlew run --no-daemon

# Terminal 2: Manual API test
curl http://localhost:8080/events/devlille-2025/partnerships/<uuid> | jq .

# Verify response includes:
# - partnership.currency
# - partnership.validated_pack.total_price
# - partnership.validated_pack.optional_options[].complete_description
```

---

## Common Issues & Solutions

### Issue 1: Circular Schema References

**Symptom**: `npm run validate` fails with "Circular $ref detected"

**Solution**:
- Ensure `partnership_option.schema.json` uses `$defs` for subtypes, not separate files
- Use `oneOf` discriminator pattern at root level
- Verify `partnership_pack.schema.json` references option schema correctly

### Issue 2: Missing Translation Exception

**Symptom**: Tests fail with `ForbiddenException: Option X does not have a translation`

**Solution**:
```kotlin
// In test setup, ensure all options have translations
val option = insertMockedSponsoringOption()
OptionTranslationEntity.new {
    this.option = option
    this.language = "en"
    this.name = "Test Option"
    this.description = "Test description"
}
```

### Issue 3: Total Price Mismatch

**Symptom**: Tests fail with incorrect `totalPrice` calculation

**Solution**:
- Verify required options are NOT included in total (price = null)
- Check optional option calculations: `price × quantity`
- Ensure selectable options use `selectedValue.price`

### Issue 4: Complete Description Format

**Symptom**: Complete description missing parentheses or wrong format

**Solution**:
```kotlin
// Correct format (parentheses separator)
"Conference passes (5 attendees)"  // ✅
"Conference passes: 5 attendees"   // ❌ (colon format deprecated)

// Edge case: zero quantity
"Conference passes"  // ✅ (omit suffix if quantity = 0)
```

---

## Development Timeline

**Estimated Breakdown**:
1. **Domain Models** (1-2 hours): Create PartnershipOption, update Detail/Pack
2. **Repository Logic** (2-3 hours): Implement mapping, pricing calculations
3. **Endpoint Updates** (1 hour): Update billing/agreement routes
4. **JSON Schemas** (1 hour): Create/update schemas, update openapi.yaml
5. **Testing** (2-3 hours): Update existing tests, add new assertions

**Total**: 5-8 hours

---

## Deployment Notes

**Database**: No migrations required (using existing schema).

**Breaking Changes**: 
- `PartnershipPack.options` type changed from `SponsoringOption` to `PartnershipOption`
- Response includes new fields (backward compatible for additive changes)
- User explicitly waived backward compatibility concerns

**Rollback Plan**:
- Revert to previous commit if issues arise
- No database state changes to rollback

---

## Next Steps After Implementation

1. **Code Review**: Submit PR for review, ensure CI passes
2. **Frontend Updates**: 
   - Run `pnpm orval` in `/front` to regenerate TypeScript client
   - Update components consuming partnership detail
3. **Documentation**: Update API docs with new response structure
4. **Monitoring**: Verify response times <500ms in production

---

## References

- **Specification**: `specs/013-partnership-option-display/spec.md`
- **Data Model**: `specs/013-partnership-option-display/data-model.md`
- **Research**: `specs/013-partnership-option-display/research.md`
- **Constitution**: `.specify/constitution.md`
- **OpenAPI Spec**: `server/application/src/main/resources/openapi.yaml`
