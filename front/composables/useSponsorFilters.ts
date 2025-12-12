import { ref, computed, watchEffect, type Ref, type ComputedRef } from "vue";
import type { GetOrgsEventsPartnershipParams } from "~/utils/api";
import { initialFilterState, FilterStateSchema, type FilterState } from "~/types/sponsors";

/**
 * Options for useSponsorFilters composable
 *
 * @interface UseSponsorFiltersOptions
 * @property {string} orgSlug - Organization slug from route params
 * @property {string} eventSlug - Event slug from route params
 */
export interface UseSponsorFiltersOptions {
  /** Organization slug */
  orgSlug: string;
  /** Event slug */
  eventSlug: string;
}

/**
 * Return type for useSponsorFilters composable
 *
 * @interface UseSponsorFiltersReturn
 * @property {Ref<FilterState>} filters - Current reactive filter state
 * @property {ComputedRef<number>} activeFilterCount - Number of active filters (non-null values)
 * @property {ComputedRef<boolean>} isEmpty - True when no filters are active
 * @property {ComputedRef<GetOrgsEventsPartnershipParams>} queryParams - API query parameters derived from filter state
 * @property {Function} setPackFilter - Update pack filter value
 * @property {Function} setStatusFilter - Update a single status filter value
 * @property {Function} clearAllFilters - Reset all filters to initial state
 * @property {Function} clearFilter - Clear a specific filter by key
 */
export interface UseSponsorFiltersReturn {
  /** Current filter state (reactive) */
  filters: Ref<FilterState>;
  /** Number of active (non-null) filters */
  activeFilterCount: ComputedRef<number>;
  /** True if no filters are active */
  isEmpty: ComputedRef<boolean>;
  /** API query parameters derived from filter state */
  queryParams: ComputedRef<GetOrgsEventsPartnershipParams>;
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
 * Composable for managing sponsor list filters
 *
 * Provides reactive filter state, computed properties for active filter count and API query params,
 * and action methods for updating filters. Filters are persisted to sessionStorage with event-specific keys.
 *
 * @param options - Configuration options including orgSlug and eventSlug
 * @returns Filter state management interface
 *
 * @example
 * ```typescript
 * const { filters, setPackFilter, queryParams } = useSponsorFilters({
 *   orgSlug: 'my-org',
 *   eventSlug: 'my-event'
 * })
 *
 * setPackFilter('pack-123')
 * console.log(queryParams.value) // { 'filter[pack_id]': 'pack-123' }
 * ```
 */
export function useSponsorFilters(options: UseSponsorFiltersOptions): UseSponsorFiltersReturn {
  const { orgSlug, eventSlug } = options;
  const storageKey = `sponsor-filters:${orgSlug}:${eventSlug}`;

  const route = useRoute();
  const router = useRouter();

  // Initialize filter state - restore from URL or sessionStorage immediately
  function getInitialFilters(): FilterState {
    if (typeof window === "undefined") return { ...initialFilterState };

    const query = route.query;

    // Restore from URL first (takes priority over sessionStorage)
    if (Object.keys(query).length > 0) {
      const urlFilters: Partial<FilterState> = {};

      if (query.packId) urlFilters.packId = query.packId as string;
      if (query.validated !== undefined) urlFilters.validated = query.validated === "true";
      if (query.paid !== undefined) urlFilters.paid = query.paid === "true";
      if (query.agreementGenerated !== undefined)
        urlFilters.agreementGenerated = query.agreementGenerated === "true";
      if (query.agreementSigned !== undefined)
        urlFilters.agreementSigned = query.agreementSigned === "true";
      if (query.suggestion !== undefined) urlFilters.suggestion = query.suggestion === "true";

      return { ...initialFilterState, ...urlFilters };
    }

    // Fallback to sessionStorage if no URL params
    const stored = sessionStorage.getItem(storageKey);
    if (!stored) return { ...initialFilterState };

    try {
      const parsed = JSON.parse(stored);
      const result = FilterStateSchema.safeParse(parsed);

      if (result.success) {
        return result.data;
      } else {
        console.warn("[useSponsorFilters] Invalid filter state in storage, using defaults");
        return { ...initialFilterState };
      }
    } catch (error) {
      console.error("[useSponsorFilters] Failed to parse filter state from storage:", error);
      return { ...initialFilterState };
    }
  }

  const filters = ref<FilterState>(getInitialFilters());

  /**
   * Count of active (non-null) filters
   */
  const activeFilterCount = computed<number>(() => {
    return Object.values(filters.value).filter((value) => value !== null).length;
  });

  /**
   * True if no filters are currently active
   */
  const isEmpty = computed<boolean>(() => {
    return activeFilterCount.value === 0;
  });

  // Sync filters to URL and sessionStorage when they change
  watchEffect(() => {
    // Only run on client-side
    if (typeof window === "undefined") return;

    // Update URL query params
    const query: Record<string, string> = {};
    if (filters.value.packId) query.packId = filters.value.packId;
    if (filters.value.validated !== null) query.validated = String(filters.value.validated);
    if (filters.value.paid !== null) query.paid = String(filters.value.paid);
    if (filters.value.agreementGenerated !== null)
      query.agreementGenerated = String(filters.value.agreementGenerated);
    if (filters.value.agreementSigned !== null)
      query.agreementSigned = String(filters.value.agreementSigned);
    if (filters.value.suggestion !== null) query.suggestion = String(filters.value.suggestion);

    router.replace({ query });

    // Update sessionStorage
    if (isEmpty.value) {
      sessionStorage.removeItem(storageKey);
    } else {
      sessionStorage.setItem(storageKey, JSON.stringify(filters.value));
    }
  });

  /**
   * API query parameters derived from current filter state
   * Maps FilterState to GetOrgsEventsPartnershipParams format
   */
  const queryParams = computed<GetOrgsEventsPartnershipParams>(() => {
    const params: GetOrgsEventsPartnershipParams = {};

    if (filters.value.packId !== null) {
      params["filter[pack_id]"] = filters.value.packId;
    }

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
   * @param value New value (null = show both)
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
    filters.value = { ...initialFilterState };
  }

  /**
   * Clear a specific filter (set to null)
   * @param key Which filter to clear
   */
  function clearFilter(key: keyof FilterState): void {
    filters.value[key] = null;
  }

  return {
    filters,
    activeFilterCount,
    isEmpty,
    queryParams,
    setPackFilter,
    setStatusFilter,
    clearAllFilters,
    clearFilter,
  };
}
