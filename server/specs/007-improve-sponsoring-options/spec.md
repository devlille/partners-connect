# Feature Specification: Enhanced Sponsoring Options with Four Option Types

**Feature Branch**: `007-improve-sponsoring-options`  
**Created**: 31 October 2025  
**Status**: Draft  
**Input**: User description: "improve sponsoring options to have 4 kind of options: text (actual implementation) and 3 new ones: typed quantitative option to select an option with a number and a type (number of job offers), typed number option to select an option with a type (number of tickets) but with a fixed number configured by the organizer and booth option to select an option with a booth configuration (the size of the booth like 3x3m, 3x6m, ...). An organizer can create multiple options in an event and attach them to one or more packs (like it is already the case but with text option only). When a partner create a partnership based on pack and options availables, they will need to pick quantity and/or type according to the type of the option selected."

## Clarifications

### Session 2025-10-31
- Q: How should pricing work for typed quantitative options when partners select different quantities? → A: Each additional unit costs the same as the base price (linear scaling)
- Q: What should happen when a partner tries to select a quantity of 0 for a typed quantitative option? → A: Treat 0 quantity as "not selecting this option" (skip it entirely)
- Q: How should the system handle when an organizer removes a selectable value that's already chosen by existing partnerships? → A: Block deletion with error message until all partnerships are updated
- Q: What validation should apply to quantity input for typed quantitative options? → A: No upper limit - partners can select any positive integer
- Q: How should the system handle typed selectable options with no available values defined by the organizer? → A: Prevent option creation until at least one selectable value is added

## User Scenarios & Testing

### Primary User Story
As an event organizer, I want to create different types of sponsoring options beyond simple text descriptions, so that I can offer more specific and measurable benefits to sponsors. As a partner company, I want to select specific quantities and predefined values for sponsoring options that match my business needs.

### Acceptance Scenarios
1. **Given** an organizer is creating a sponsoring option, **When** they select "text" type, **Then** they can provide translations with name/description only (current behavior)
2. **Given** an organizer is creating a sponsoring option, **When** they select "typed quantitative" type, **Then** they must provide translations with name/description AND specify a type descriptor (e.g., "job offers"), and partners can select a quantity
3. **Given** an organizer is creating a sponsoring option, **When** they select "typed number" type, **Then** they must provide translations with name/description AND specify a type descriptor (e.g., "tickets") and a fixed quantity, and partners receive that exact amount
4. **Given** an organizer is creating a sponsoring option, **When** they select "typed selectable" type, **Then** they must provide translations with name/description AND specify a type descriptor (e.g., "booth") and available selectable values (e.g., "3x3m", "3x6m", "Premium", "Standard"), and partners can select one value
5. **Given** a partner is creating a partnership, **When** they select a pack with typed quantitative options, **Then** they must specify the desired quantity for each selected option
6. **Given** a partner is creating a partnership, **When** they select a pack with typed selectable options, **Then** they must choose one value from the available predefined options
7. **Given** a partner is creating a partnership, **When** they select a pack with typed number options, **Then** the fixed quantity is automatically applied without user input needed

### Edge Cases
- What happens when an organizer tries to delete an option type that's already used in existing partnerships?
- How does the system handle typed selectable options with no available values defined? (Resolved: prevent creation until values added)
- How does pricing work for different quantities of the same option? (Resolved: linear scaling)
- What happens when an organizer removes a selectable value that's already chosen by existing partnerships? (Resolved: block deletion with error)

## Requirements

### Functional Requirements
- **FR-001**: System MUST support four distinct sponsoring option types: text, typed quantitative, typed number, and typed selectable
- **FR-002**: System MUST allow organizers to specify option type when creating new sponsoring options
- **FR-003**: All option types MUST maintain the existing multilingual translation system with name and description fields
- **FR-004**: For typed quantitative options, organizers MUST be able to define a type descriptor (e.g., "job offers", "social media posts") in addition to translations
- **FR-005**: For typed number options, organizers MUST be able to define both a type descriptor and a fixed quantity value in addition to translations
- **FR-006**: For typed selectable options, organizers MUST be able to define a type descriptor (e.g., "booth", "sponsorship level") and multiple selectable string values (e.g., "3x3m", "3x6m", "Premium", "Standard") in addition to translations
- **FR-007**: Partners MUST be able to specify quantities when selecting typed quantitative options during partnership creation
- **FR-021**: System MUST require at least one selectable value before allowing creation of typed selectable options
- **FR-008**: Partners MUST be able to choose from predefined string values when selecting typed selectable options during partnership creation
- **FR-009**: System MUST automatically apply fixed quantities for typed number options without partner input
- **FR-010**: Text options MUST continue to work exactly as they currently do (backward compatibility)
- **FR-011**: System MUST validate that partners provide required selections (quantities/values) based on option types
- **FR-012**: System MUST persist partner selections (quantities, selected values) with the partnership data
- **FR-018**: For typed quantitative options, system MUST treat quantity of 0 as "option not selected" and exclude it from the partnership
- **FR-020**: For typed quantitative options, system MUST accept any positive integer quantity with no upper limit validation
- **FR-013**: System MUST prevent deletion of option types that are referenced by existing partnerships
- **FR-014**: Organizers MUST be able to attach all four option types to sponsoring packs using existing pack-option association system
- **FR-019**: System MUST prevent deletion of selectable values that are chosen by existing partnerships and display error message with affected partnerships
- **FR-017**: For typed quantitative options, system MUST calculate total cost using linear scaling (base_price × selected_quantity)
- **FR-015**: System MUST display appropriate UI controls based on option type when partners create partnerships
- **FR-016**: All option types MUST be displayable as text-based options with their translations, ensuring compatibility with existing display logic

### Key Entities
- **SponsoringOption**: Extended to include option type while maintaining existing translations (name/description), plus optional type descriptor, fixed quantity (for typed number), and selectable values (for typed selectable)
- **PartnershipOption**: Extended to include selected quantity (for typed quantitative) and selected string value (for typed selectable) while preserving existing text-based display capabilities
- **OptionType**: New enumeration defining the four supported types (TEXT, TYPED_QUANTITATIVE, TYPED_NUMBER, TYPED_SELECTABLE)
- **SelectableValue**: Represents available string values that partners can choose from for typed selectable options

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous  
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed
