# Feature Specification: Display Partnership-Specific Options

**Feature Branch**: `013-partnership-option-display`  
**Created**: November 30, 2025  
**Status**: Draft  
**Input**: User description: "When we display a partnership with selected pack and options, we should display partnership options instead of sponsoring options, meaning that we aren't interested about all available values for selectable options, only interested about options selected and required in the original pack, values picked by the user and translation of options in the language of the partnership."

## Clarifications

### Session 2025-11-30

- Q: Who can access partnership details (view/edit)? → A: Partnership details are public (no authentication required)
- Q: What is the exact format for complete descriptions (separator between description and value)? → A: Use parentheses for value (e.g., "Description (value)")
- Q: How should the system handle partnerships when option translations are missing for the partnership's language? → A: Return error and fail to display the partnership
- Q: Should there be a maximum length for complete descriptions? → A: Allow unlimited length (no maximum enforced)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Partnership with Complete Option Details (Priority: P1)

An organizer views a partnership detail page and sees only the options that are relevant to this specific partnership: required options from the pack plus the optional options the partner selected, with values they chose, all displayed in the partnership's language. Each option includes both structured data (original option description, picked values, quantities) and a human-readable complete description that merges the option description with the selected value.

**Why this priority**: This is the core value proposition - showing organizers the actual partnership configuration without overwhelming them with all possible pack options that weren't selected. The complete description enables seamless use in document generation (quotes, invoices, agreements) while structured properties support programmatic access and data validation.

**Independent Test**: Can be fully tested by viewing any partnership detail page and verifying that only selected/required options appear with their specific values, complete descriptions, and structured properties that can be used for document generation.

**Acceptance Scenarios**:

1. **Given** a partnership has a selected pack with 3 required options and 5 optional options available, **When** the partner selected 2 of the optional options during registration, **Then** the partnership detail displays 5 options total (3 required + 2 selected optional), not all 8
2. **Given** a partnership includes a quantitative option (e.g., "Job offers posting on website") with user-selected quantity of 3, **When** viewing the partnership detail, **Then** each option displays: (a) original description "Publish job offers on event website", (b) selected quantity 3, (c) complete description "Publish job offers on event website (3 positions)"
3. **Given** a partnership includes a selectable option (e.g., "Exhibition booth location") with description "Choose your booth location in the exhibition hall" where partner chose "Stand A1 - Corner booth near entrance", **When** viewing the partnership detail, **Then** each option displays: (a) original description "Choose your booth location in the exhibition hall", (b) selected value "Stand A1 - Corner booth near entrance", (c) complete description "Choose your booth location in the exhibition hall (Stand A1 - Corner booth near entrance)"
4. **Given** a partnership includes a number option (e.g., "Conference passes") with description "Access passes for conference attendees" and fixed quantity 10, **When** viewing the partnership detail, **Then** each option displays: (a) original description "Access passes for conference attendees", (b) fixed quantity 10, (c) complete description "Access passes for conference attendees (10 passes)"
5. **Given** a partnership includes a text option (e.g., "Company logo on website") with description "Display company logo on event homepage", **When** viewing the partnership detail, **Then** each option displays: (a) original description "Display company logo on event homepage", (b) no quantity or value (text option), (c) complete description same as original "Display company logo on event homepage"
6. **Given** a partnership has language set to "fr", **When** viewing the partnership detail, **Then** all option names, original descriptions, and complete descriptions display in French using the French translations
7. **Given** an organizer generates a quote or invoice from partnership details, **When** creating line items, **Then** the complete description can be used directly as the item description without additional string concatenation or formatting
8. **Given** an organizer generates an agreement from partnership details, **When** mapping partnership data to agreement model, **Then** all necessary option information (name, original description, selected values, complete description) is available without additional database queries

---

### User Story 2 - View Complete Partnership Pricing Breakdown (Priority: P2)

An organizer views partnership details and sees the complete pricing breakdown including pack base price, required options (included), optional options (additional cost), individual option pricing with quantities/selected values, and total partnership amount.

**Why this priority**: Provides complete financial transparency for the partnership, enabling organizers to understand total cost, pricing components, and prepare invoices/quotes without separate pricing calculations. This eliminates the need for a separate pricing computation endpoint.

**Independent Test**: Can be tested by viewing a partnership with mixed option types and verifying that all pricing information is complete: pack base price, per-option amounts (unit price × quantity for quantitative, selected value price for selectable), and accurate total amount.

**Acceptance Scenarios**:

1. **Given** a partnership has a pack with base price €1000, 2 required options, and 3 selected optional options (1 quantitative with quantity 3 at €50/unit, 1 selectable with selected value at €200, 1 text at €100), **When** viewing the partnership detail, **Then** display shows: pack base price €1000, required options with €0 additional cost, optional options totaling €450 (€150 + €200 + €100), and total partnership amount €1450
2. **Given** a partnership with a quantitative option (job offers at €50/unit) where partner selected quantity 5, **When** viewing the detail, **Then** display shows unit amount €50, quantity 5, and total option amount €250
3. **Given** a partnership with a selectable option (booth location) where partner chose "Large booth" priced at €300, **When** viewing the detail, **Then** display shows selected value "Large booth" with amount €300
4. **Given** a partnership with only required options and no optional selections, **When** viewing the detail, **Then** display shows pack base price equals total amount with no additional costs
5. **Given** an organizer needs to generate an invoice or quote for the partnership, **When** they view the partnership detail, **Then** all necessary pricing information (line items, quantities, amounts, total) is available without needing to call a separate pricing computation endpoint

---

### User Story 3 - Edit Partnership with Correct Options (Priority: P3)

An organizer edits a partnership and sees the same partnership-specific options (not all pack options) pre-populated in the edit form.

**Why this priority**: Ensures consistency between view and edit modes, preventing confusion about which options are actually part of the partnership.

**Independent Test**: Can be tested by opening edit mode for any partnership and verifying pre-populated options match the partnership's actual selections.

**Acceptance Scenarios**:

1. **Given** a partnership has 3 required and 2 selected optional options, **When** organizer opens the edit form, **Then** the form shows only those 5 options pre-populated with their current values
2. **Given** a partnership has a selectable option with value "Stand A1" selected, **When** opening the edit form, **Then** the selectable field shows "Stand A1" as the current selection
3. **Given** a partnership has a quantitative option with quantity 5, **When** opening the edit form, **Then** the quantity field displays "5" as the current value

---

### Edge Cases

- What happens when a partnership has no optional options selected (only required options from pack)?
- How does system handle partnerships created before option translations were added for the partnership's language?
- What happens when an option was deleted from the system but still referenced by the partnership?
- What happens when a selectable option's selected value ID no longer exists in the available values?
- How does system handle pricing calculation when option prices have changed since partnership creation?
- What happens when currency conversion is needed but partnership was created with different currency assumptions?
- How does system format complete descriptions when quantity values are zero or null?
- What happens when selected value names contain special characters that might break document formatting?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST return partnership-specific option data including only required pack options and user-selected optional options
- **FR-002**: System MUST include user's selected values for each option type (quantity for quantitative, value for selectable, etc.)
- **FR-003**: System MUST return option translations in the partnership's language, not all available translations
- **FR-004**: For selectable options, system MUST return only the selected value's details (ID, name, price) not all available selectable values
- **FR-005**: For quantitative options, system MUST return the selected quantity value
- **FR-006**: For number options, system MUST return the fixed quantity from the option definition
- **FR-007**: System MUST distinguish between required options (included in base pack price) and optional options (additional cost)
- **FR-008**: Frontend MUST display partnership options without fetching all pack options from the sponsoring packs API
- **FR-009**: Partnership detail response MUST include a dedicated `pack_options` collection containing partnership-specific option data
- **FR-010**: Each partnership option MUST include: option ID, translated name, translated description, price (null for required, actual for optional), option type, and type-specific selection data
- **FR-011**: System MUST calculate and return complete pricing breakdown including pack base price, currency, total partnership amount
- **FR-012**: System MUST return per-option pricing details including unit amount, quantity, total option amount, and selected value for pricing calculations
- **FR-013**: System MUST compute total partnership amount as: base pack price + sum of all optional option amounts
- **FR-014**: For quantitative options, system MUST calculate option amount as: unit price × selected quantity
- **FR-015**: For selectable options, system MUST use the selected value's specific price for option amount calculation
- **FR-016**: For number options, system MUST calculate option amount using the fixed quantity from the option definition
- **FR-017**: Partnership detail response MUST provide all pricing information necessary for invoice and quote generation without requiring separate pricing endpoint calls
- **FR-018**: Each partnership option MUST include a complete description that merges the original option description with the selected value in a human-readable format
- **FR-019**: For quantitative options, complete description MUST append the selected quantity in parentheses (e.g., "Option description (5 units)")
- **FR-020**: For selectable options, complete description MUST append the selected value name in parentheses (e.g., "Option description (Selected value name)")
- **FR-021**: For number options, complete description MUST append the fixed quantity in parentheses (e.g., "Option description (10 items)")
- **FR-022**: For text options, complete description MUST be identical to the original description (no appended value)
- **FR-023**: Each partnership option MUST maintain separate structured properties: original description, selected quantity/value, and complete description
- **FR-024**: Complete descriptions MUST be usable directly as line item descriptions in invoices, quotes, and agreements without additional formatting
- **FR-025**: System MUST preserve both structured data (for programmatic access) and formatted complete descriptions (for document generation) in the same response
- **FR-026**: Partnership detail endpoint MUST be publicly accessible without authentication requirements
- **FR-027**: System MUST return an error (forbidden exception) when option translations are missing for the partnership's language
- **FR-028**: System MUST allow unlimited length for complete descriptions without enforcing maximum character limits

### Key Entities *(include if feature involves data)*

- **Partnership**: The collaboration agreement between a company and an event, includes a selected pack and chosen options with their values, plus complete pricing breakdown
- **Partnership Option**: A specific option selected for a partnership, includes the option's metadata plus user's selection data (quantity, selected value, etc.), pricing details (unit amount, total amount), original description, and complete formatted description ready for document generation
- **Sponsoring Pack**: The tier/package chosen for a partnership, defines which options are required and which are optional, includes base price
- **Sponsoring Option**: The benefit/service definition (e.g., logo placement, booth space), has translations in multiple languages and type-specific configuration, includes pricing information
- **Option Selection Data**: User's choices for an option - quantity for quantitative, selected value ID for selectable, etc.
- **Option Translation**: Language-specific name and description for an option
- **Partnership Pricing**: Complete financial breakdown of a partnership including pack base price, itemized option costs (with unit amounts and quantities), currency, and total partnership amount
- **Complete Option Description**: A human-readable formatted description that merges the original option description with the selected value/quantity, ready for use in invoices, quotes, and agreements without additional formatting

## Dependencies and Assumptions

### Dependencies

- Partnership data must already exist in the system with selected pack and option selections
- Option translations must exist for the partnership's language
- Partnership option entity relationships (via PartnershipOptionsTable) must be established during partnership creation
- Pack-option relationships (via PackOptionsTable) must define which options are required vs. optional
- Option pricing information must be available for all option types

### Assumptions

- Partnership option selections are stored with sufficient detail (quantity, selected value IDs) to reconstruct the partnership-specific view
- Required vs. optional option distinction is maintained at the pack level (via PackOptionsTable)
- Option translations are complete for all supported partnership languages
- The current polymorphic option type system (text, typed_quantitative, typed_number, typed_selectable) remains stable
- Frontend currently makes separate API calls to fetch pack options for display, creating unnecessary overhead
- A separate pricing computation endpoint currently exists and calculates pricing information (pack base price, option amounts, totals) on demand
- Invoice and quote generation currently depends on separate pricing computation calls
- Agreement generation currently performs separate database queries to fetch partnership option details
- Option prices are stored at creation time and do not change retroactively for existing partnerships
- Currency is consistent across all options within a partnership (typically EUR)
- Pricing calculations follow consistent formulas: quantitative (unit price × quantity), selectable (selected value price), number (unit price × fixed quantity), text (flat price)
- Complete descriptions will use a consistent format: "Original description (Selected value/quantity)" using parentheses as the separator
- Document generation systems (invoices, quotes, agreements) can consume complete descriptions as-is without additional formatting logic
- Structured properties (original description, selected values) remain accessible alongside complete descriptions for systems that need granular access

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Partnership detail pages load without making additional API calls to fetch full pack options (reduce API calls by 1 per partnership view)
- **SC-002**: Organizers viewing partnership details see only relevant options (5-8 options typical) instead of all pack options (10-15+ options), reducing visual clutter by 40-50%
- **SC-003**: Partnership options display in correct language 100% of the time matching partnership's language setting
- **SC-004**: For selectable options, users see single selected value instead of full dropdown list, reducing cognitive load
- **SC-005**: Edit forms for partnerships pre-populate with correct option values without additional data fetching or transformation logic
- **SC-006**: Invoice and quote generation processes eliminate the need for separate pricing computation API calls (reduce API calls by 1 per billing operation)
- **SC-007**: Partnership pricing calculations are accurate 100% of the time with correct totals (base price + sum of optional option amounts)
- **SC-008**: Organizers can view complete pricing breakdown (pack price, per-option costs, total) directly in partnership details without navigating to separate pricing views
- **SC-009**: Complete option descriptions are usable directly in document generation without string manipulation (100% of generated documents use pre-formatted descriptions)
- **SC-010**: Agreement generation eliminates separate database queries for option details (reduce queries by 1-3 per agreement generation)
- **SC-011**: All partnership option information (structured properties and complete descriptions) is available in a single API response, supporting both human display and automated document generation
