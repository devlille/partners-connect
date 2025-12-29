# Research: Display Partnership-Specific Options

**Date**: November 30, 2025  
**Phase**: 0 (Outline & Research)

## Research Questions

### 1. Complete Description Format Implementation

**Question**: How should the complete description be generated for each option type (quantitative, selectable, number, text)?

**Decision**: Use parentheses format with type-specific value formatting:
- **Quantitative**: `"{description} ({quantity})"` - e.g., "Publish job offers on event website (3)"
- **Selectable**: `"{description} ({selectedValue})"` - e.g., "Choose your booth location (Stand A1 - Corner booth near entrance)"
- **Number**: `"{description} ({fixedQuantity})"` - e.g., "Access passes for conference attendees (10)"
- **Text**: `"{description}"` (unchanged) - e.g., "Display company logo on event homepage"

**Rationale**: 
- Parentheses format specified in clarifications (Question 2 from clarify session)
- Consistent with document generation requirements (invoices, quotes, agreements)
- Avoids colon separator which could conflict with structured data formats
- Human-readable while remaining machine-parseable

**Alternatives Considered**:
- Colon separator (`"description: value"`) - Rejected: Could conflict with YAML/structured formats
- Dash separator (`"description - value"`) - Rejected: Less clear semantic separation
- Newline separator - Rejected: Breaks single-line requirement for document line items

**Implementation Notes**:
- Create extension function on `PartnershipOptionEntity` to generate complete description
- Use translation from partnership language for both description and unit labels
- Handle null/zero quantities by omitting quantity suffix (edge case scenario)

---

### 2. Pricing Calculation in Repository vs Domain Model

**Question**: Should pricing calculations happen in repository layer or be properties on domain models?

**Decision**: Pricing calculations happen in repository layer during entity-to-domain mapping.

**Rationale**:
- Existing pattern in `PartnershipBillingRepositoryExposed.computePricing()` already calculates pricing in repository
- Repository has access to all required data (option prices, quantities, selected values, pack base price)
- Domain models remain pure data structures without calculation logic
- Consistent with Exposed ORM pattern where calculations happen during query/mapping

**Alternatives Considered**:
- Domain model methods (e.g., `PartnershipOption.calculateAmount()`) - Rejected: Domain models are data classes, calculations belong in application layer
- Separate pricing service - Rejected: Overkill for straightforward calculations, creates unnecessary abstraction

**Implementation Notes**:
- Enhance `PartnershipRepositoryExposed.getByIdDetailed()` to include pricing calculations
- Reuse existing pricing logic from `PartnershipBillingRepositoryExposed` 
- Calculate per-option amounts based on type: quantitative (price × quantity), selectable (selected value price), number (price × fixed quantity), text (flat price)
- Calculate total as: pack base price + sum of optional option amounts

---

### 3. Backward Compatibility for Invoice/Quote/Agreement Endpoints

**Question**: How should invoice, quote, and agreement endpoints transition from separate pricing/data queries to using enhanced partnership detail?

**Decision**: Update invoice/quote/agreement route handlers to call `partnershipRepository.getByIdDetailed()` and extract needed data from the enhanced response instead of calling separate `computePricing()` or making additional queries.

**Rationale**:
- User requirement explicitly states "don't worry about backward compatibility" for API responses
- Consolidates data fetching into single source of truth (partnership detail)
- Eliminates duplicate pricing calculations across endpoints
- Reduces API calls as specified in success criteria (SC-006, SC-010)

**Alternatives Considered**:
- Keep separate pricing endpoint - Rejected: Violates DRY principle, creates data inconsistency risk
- Create adapter layer for backward compatibility - Rejected: User explicitly waived backward compatibility requirement

**Implementation Notes**:
- Update `BillingRoutes.kt` invoice/quote endpoints to fetch partnership detail first
- Map enhanced `PartnershipDetail` to existing `PartnershipPricing` structure for `BillingRepository`
- Update `PartnershipAgreementRepositoryExposed` to accept `PartnershipDetail` instead of performing separate queries
- Update affected tests to expect single repository call instead of multiple calls

---

### 4. JSON Schema Structure for Partnership Options

**Question**: How should the partnership option schema be structured to support polymorphic option types while including complete description and pricing?

**Decision**: Create a single `PartnershipOptionSchema` that includes:
- Common fields: `id`, `name`, `description` (original), `complete_description`, `price`, `required`
- Type discriminator: `type` field with values `text`, `typed_quantitative`, `typed_number`, `typed_selectable`
- Type-specific fields: `selected_quantity`, `fixed_quantity`, `selected_value` (conditional based on type)
- Pricing fields: `price` (unit price), `total_price`, `quantity`

**Rationale**:
- Matches existing `SponsoringOptionSchema` polymorphic pattern using `@JsonClassDiscriminator("type")`
- OpenAPI 3.1.0 compatible with conditional field requirements
- Supports both structured access (for programmatic use) and formatted display (for documents)
- Single schema prevents duplication across invoice/quote/agreement schemas

**Alternatives Considered**:
- Separate schemas per option type - Rejected: Creates schema duplication, harder to maintain
- Flatten all fields into single object - Rejected: Loses type safety and validation benefits
- Nested pricing object - Rejected: Adds unnecessary nesting for simple fields

**Implementation Notes**:
- Create `partnership_option.schema.json` in `server/application/src/main/resources/schemas/`
- Use `oneOf` with `type` discriminator for polymorphic validation
- Reference from `partnership_pack.schema.json` for options array
- Ensure Orval generates correct TypeScript types for frontend

---

### 5. Missing Translation Error Handling

**Question**: What exception should be thrown when option translations are missing for the partnership's language?

**Decision**: Throw `ForbiddenException` with descriptive message, consistent with existing pattern in `PartnershipRepositoryExposed.create()` and `PartnershipSuggestionRepositoryExposed.suggest()`.

**Rationale**:
- Clarification session (Question 3) confirmed: throw error and fail to display partnership
- Existing code already uses `ForbiddenException` for missing translations (see lines 105-110 in `PartnershipRepositoryExposed.kt`)
- StatusPages automatically converts to HTTP 403 response
- Prevents partial/incomplete data from being returned to clients

**Alternatives Considered**:
- Return null for missing translations - Rejected: User explicitly requested failure (Option A in clarification)
- Use NotFoundException - Rejected: Inconsistent with existing code pattern
- Use custom TranslationMissingException - Rejected: Unnecessary new exception type

**Implementation Notes**:
- Check for translation existence in `PartnershipRepositoryExposed.getByIdDetailed()` 
- Message format: `"Option {optionId} does not have a translation for language {language}"`
- Same validation used in partnership creation already ensures translations exist, so this is defensive programming
- Add test case for missing translation scenario

---

## Technology Stack Confirmation

### Kotlin & Ktor Framework
- **Version**: Kotlin with JVM 21, Ktor 2.x
- **Serialization**: kotlinx.serialization for JSON (no manual Jackson configuration)
- **DI**: Koin for dependency injection
- **Status**: Already in use, no new dependencies needed

### Exposed ORM
- **Pattern**: Dual Table/Entity structure (UUIDTable + UUIDEntity)
- **Date handling**: `datetime()` columns (not `timestamp()`)
- **Enum handling**: `enumerationByName<EnumType>()`
- **Status**: Existing entities will be reused (PartnershipEntity, PartnershipOptionEntity, SponsoringOptionEntity)

### OpenAPI & JSON Schema
- **Standard**: OpenAPI 3.1.0 compatible schemas
- **Schema location**: `server/application/src/main/resources/schemas/`
- **Validation**: `npm run validate` for schema correctness
- **Documentation**: `npm run bundle` to generate final documentation.yaml
- **Status**: Will create new schemas for partnership option, update existing schemas

### Testing Framework
- **Integration tests**: Kotlin test with H2 in-memory database
- **Contract tests**: Schema validation via `call.receive<T>(schema)` pattern (not applicable for GET endpoints)
- **Mock factories**: Existing pattern in `PartnershipMockFactories.kt`
- **Status**: Will update existing tests, no new test frameworks needed

---

## Best Practices Applied

### Repository Layer Best Practices
1. **No repository dependencies**: `PartnershipRepositoryExposed` will not inject other repositories
2. **Exposed entities only**: Use `PartnershipOptionEntity.listByPartnershipAndPack()` directly
3. **Exception-based errors**: Throw `ForbiddenException`, `NotFoundException` - no nullable returns
4. **Single responsibility**: Repository fetches and maps data, routes orchestrate cross-domain operations

### Domain Model Best Practices
1. **Immutable data classes**: Use `data class` with `val` properties
2. **Sealed classes for polymorphism**: Follow existing `SponsoringOption` sealed class pattern
3. **SerialName annotations**: Use `@SerialName("snake_case")` for JSON compatibility
4. **KDoc documentation**: Document all public APIs with clear descriptions

### OpenAPI Best Practices
1. **Schema components**: Reference external JSON schema files, never inline schemas
2. **oneOf for polymorphism**: Use discriminator field for type-based validation
3. **Slug vs ID**: Use `event_slug` in responses (not `event_id`)
4. **Security definitions**: Public endpoint uses `security: - {}`

### Test Best Practices
1. **Integration over unit**: Test via HTTP routes, not individual repository methods
2. **H2 isolation**: Each test uses transaction rollback for database cleanup
3. **Mock factories**: Reuse existing `mockCompany()`, `mockEvent()` patterns
4. **Coverage target**: Minimum 80% for new/modified code

---

## Risk Assessment

### Low Risk Items
- ✅ **Schema changes**: Additive only (new fields to existing models)
- ✅ **Existing tests**: Clear update path (change assertions to expect new fields)
- ✅ **OpenAPI validation**: Existing `npm run validate` catches schema errors

### Medium Risk Items
- ⚠️ **Complete description format**: Ensure unit labels are translated correctly for all languages
- ⚠️ **Pricing calculations**: Must match existing `computePricing()` logic exactly to maintain invoice accuracy
- ⚠️ **Frontend compatibility**: Orval regeneration might affect existing frontend code (acceptable per user)

### Mitigation Strategies
1. **Complete descriptions**: Add comprehensive test cases for all option types × supported languages
2. **Pricing accuracy**: Extract shared pricing calculation functions to avoid duplication
3. **Frontend impact**: Document Orval regeneration requirement in quickstart guide

---

## Dependencies & Integration Points

### Internal Dependencies
- **PartnershipRepository**: Core interface to be enhanced
- **PartnershipOptionEntity**: Existing entity for option selections
- **SponsoringOptionEntity**: Existing entity for option definitions with translations
- **BillingRepository**: Consumer of partnership pricing data
- **PartnershipAgreementRepository**: Consumer of partnership option details

### External Dependencies
- **None**: This feature is purely internal backend logic with no external service integration

### Database Schema Dependencies
- **Existing tables**: `partnerships`, `partnership_options`, `sponsoring_options`, `option_translations`, `pack_options`
- **No migrations needed**: Feature uses existing schema relationships

---

## Open Questions

None. All clarifications resolved during `/speckit.clarify` session:
- ✅ Access control: Public endpoint (no authentication)
- ✅ Description format: Parentheses separator
- ✅ Missing translations: Throw ForbiddenException
- ✅ Description length: Unlimited (no maximum enforced)
