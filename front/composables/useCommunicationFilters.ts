import { computed, type Ref, type ComputedRef } from "vue";
import type { FilterState } from "~/types/sponsors";
import { useQueryStates, parsers } from "./useQueryState";

/**
 * Options for useCommunicationFilters composable
 *
 * @interface UseCommunicationFiltersOptions
 * @property {string} orgSlug - Organization slug from route params
 * @property {string} eventSlug - Event slug from route params
 */
export interface UseCommunicationFiltersOptions {
  /** Organization slug */
  orgSlug: string;
  /** Event slug */
  eventSlug: string;
}

/**
 * Return type for useCommunicationFilters composable
 *
 * @interface UseCommunicationFiltersReturn
 * @property {Ref<FilterState>} filters - Current reactive filter state
 * @property {ComputedRef<number>} activeFilterCount - Number of active filters (non-null values)
 * @property {ComputedRef<boolean>} isEmpty - True when no filters are active
 * @property {ComputedRef<Record<string, any>>} queryParams - API query parameters derived from filter state
 * @property {Function} setPackFilter - Update pack filter value
 * @property {Function} setStatusFilter - Update a single status filter value
 * @property {Function} clearAllFilters - Reset all filters to initial state
 * @property {Function} clearFilter - Clear a specific filter by key
 */
export interface UseCommunicationFiltersReturn {
  /** Current filter state (reactive) */
  filters: Ref<FilterState>;
  /** Number of active (non-null) filters */
  activeFilterCount: ComputedRef<number>;
  /** True if no filters are active */
  isEmpty: ComputedRef<boolean>;
  /** API query parameters derived from filter state */
  queryParams: ComputedRef<Record<string, any>>;
  /** Update pack filter */
  setPackFilter(packId: string | null): void;
  /** Update a single status filter */
  setStatusFilter(
    key: "validated" | "paid" | "agreementGenerated" | "agreementSigned" | "suggestion",
    value: boolean | null,
  ): void;
  /** Clear all filters (reset to initial state) */
  clearAllFilters(): void;
  /** Clear a specific filter */
  clearFilter(key: keyof FilterState): void;
}

/**
 * Schema for communication filter query parameters
 * Uses nuqs-inspired parsers for type-safe URL state management
 * Boolean filters default to null (no filter applied)
 */
const filterSchema = {
  packId: parsers.stringOrNull(),
  validated: parsers.booleanOrNull(),
  paid: parsers.booleanOrNull(),
  agreementGenerated: parsers.booleanOrNull(),
  agreementSigned: parsers.booleanOrNull(),
  suggestion: parsers.booleanOrNull(),
  organiser: parsers.stringOrNull(),
} as const;

/**
 * Composable for managing communication filters
 *
 * Uses useQueryStates for type-safe URL query parameter management.
 * Filters are automatically synced to the URL and persisted in sessionStorage.
 *
 * @param options - Configuration options including orgSlug and eventSlug
 * @returns Filter state management interface
 *
 * @example
 * ```typescript
 * const { filters, setPackFilter, queryParams } = useCommunicationFilters({
 *   orgSlug: 'my-org',
 *   eventSlug: 'my-event'
 * })
 *
 * // Filters are automatically synced to URL
 * // e.g., ?packId=pack-123&validated=true
 *
 * setPackFilter('pack-123')
 * console.log(queryParams.value) // { 'filter[pack_id]': 'pack-123' }
 * ```
 */
export function useCommunicationFilters(
  options: UseCommunicationFiltersOptions,
): UseCommunicationFiltersReturn {
  const { orgSlug, eventSlug } = options;

  // Use useQueryStates for type-safe URL state management
  const {
    state: filters,
    resetAll,
    reset,
    modifiedCount,
    isModified,
  } = useQueryStates({
    schema: filterSchema,
    history: "replace",
    throttleMs: 50,
    storageKeyPrefix: `communication-filters:${orgSlug}:${eventSlug}`,
  });

  /**
   * Count of active (non-null) filters
   */
  const activeFilterCount = computed<number>(() => modifiedCount.value);

  /**
   * True if no filters are currently active
   */
  const isEmpty = computed<boolean>(() => !isModified.value);

  /**
   * API query parameters derived from current filter state
   * Maps FilterState to API parameter format
   * Only sends filters that have a non-null value
   */
  const queryParams = computed<Record<string, any>>(() => {
    const params: Record<string, any> = {};

    if (filters.value.packId !== null) {
      params["filter[pack_id]"] = filters.value.packId;
    }

    // Boolean filters are only sent when they have a value (not null)
    if (filters.value.validated !== null) {
      params["filter[validated]"] = filters.value.validated;
    }
    if (filters.value.paid !== null) {
      params["filter[paid]"] = filters.value.paid;
    }
    if (filters.value.agreementGenerated !== null) {
      params["filter[agreement-generated]"] = filters.value.agreementGenerated;
    }
    if (filters.value.agreementSigned !== null) {
      params["filter[agreement-signed]"] = filters.value.agreementSigned;
    }
    if (filters.value.suggestion !== null) {
      params["filter[suggestion]"] = filters.value.suggestion;
    }

    if (filters.value.organiser !== null) {
      params["filter[organiser]"] = filters.value.organiser;
    }

    return params;
  });

  /**
   * Update the pack filter
   * @param packId Pack ID to filter by, or null for all packs
   */
  function setPackFilter(packId: string | null): void {
    filters.value.packId = packId;
  }

  /**
   * Update a single status filter
   * @param key Which status filter to update
   * @param value New value (true, false, or null for no filter)
   */
  function setStatusFilter(
    key: "validated" | "paid" | "agreementGenerated" | "agreementSigned" | "suggestion",
    value: boolean | null,
  ): void {
    filters.value[key] = value;
  }

  /**
   * Clear all filters (reset to initial state)
   */
  function clearAllFilters(): void {
    resetAll();
  }

  /**
   * Clear a specific filter (set to null)
   * @param key Which filter to clear
   */
  function clearFilter(key: keyof FilterState): void {
    reset(key);
  }

  // Cast the filters ref to match the expected return type
  return {
    filters: filters as unknown as Ref<FilterState>,
    activeFilterCount,
    isEmpty,
    queryParams,
    setPackFilter,
    setStatusFilter,
    clearAllFilters,
    clearFilter,
  };
}
