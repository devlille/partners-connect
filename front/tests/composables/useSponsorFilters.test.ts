import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { nextTick } from "vue";
import { useSponsorFilters } from "~/composables/useSponsorFilters";
import { initialFilterState } from "~/types/sponsors";

// Mock useRoute and useRouter
vi.mock("vue-router", () => ({
  useRoute: () => ({
    query: {},
  }),
  useRouter: () => ({
    replace: vi.fn(),
  }),
}));

describe("useSponsorFilters", () => {
  const orgSlug = "test-org";
  const eventSlug = "test-event";
  const storageKey = `sponsor-filters:${orgSlug}:${eventSlug}`;

  beforeEach(() => {
    // Clear sessionStorage before each test
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  describe("initial state", () => {
    it("should initialize with all filters set to null except declined", () => {
      const { filters } = useSponsorFilters({ orgSlug, eventSlug });

      expect(filters.value).toEqual(initialFilterState);
      expect(filters.value.packId).toBeNull();
      // Boolean filters default to null (no filter applied) except declined
      expect(filters.value.validated).toBeNull();
      expect(filters.value.paid).toBeNull();
      expect(filters.value.agreementGenerated).toBeNull();
      expect(filters.value.agreementSigned).toBeNull();
      expect(filters.value.suggestion).toBeNull();
      expect(filters.value.declined).toBe(false);
    });

    it("should have activeFilterCount of 0 initially (declined is at default value)", () => {
      const { activeFilterCount } = useSponsorFilters({ orgSlug, eventSlug });

      // declined is false (default value), so it's not counted as modified
      expect(activeFilterCount.value).toBe(0);
    });

    it("should have isEmpty as true initially (all filters at default values)", () => {
      const { isEmpty } = useSponsorFilters({ orgSlug, eventSlug });

      // declined is at default value, so no filters are "modified"
      expect(isEmpty.value).toBe(true);
    });

    it("should have declined filter in queryParams initially", () => {
      const { queryParams } = useSponsorFilters({ orgSlug, eventSlug });

      // declined filter is sent by default with value false
      expect(queryParams.value).toEqual({
        "filter[declined]": false,
      });
    });
  });

  describe("setPackFilter", () => {
    it("should set pack filter to specified value", () => {
      const { filters, setPackFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");

      expect(filters.value.packId).toBe("pack-123");
    });

    it("should set pack filter to null when clearing", () => {
      const { filters, setPackFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");
      expect(filters.value.packId).toBe("pack-123");

      setPackFilter(null);
      expect(filters.value.packId).toBeNull();
    });

    it("should update activeFilterCount when pack filter is set", () => {
      const { activeFilterCount, setPackFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      expect(activeFilterCount.value).toBe(0); // no modified filters initially

      setPackFilter("pack-123");
      expect(activeFilterCount.value).toBe(1); // only packId
    });

    it("should update queryParams when pack filter is set", () => {
      const { queryParams, setPackFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-456");

      // pack_id and declined (default) are included
      expect(queryParams.value).toEqual({
        "filter[pack_id]": "pack-456",
        "filter[declined]": false,
      });
    });
  });

  describe("setStatusFilter", () => {
    it("should set validated filter to true", () => {
      const { filters, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("validated", true);

      expect(filters.value.validated).toBe(true);
    });

    it("should set paid filter to false", () => {
      const { filters, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("paid", false);

      expect(filters.value.paid).toBe(false);
    });

    it("should cycle agreementGenerated filter through null, true, false", () => {
      const { filters, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      // Default is null
      expect(filters.value.agreementGenerated).toBeNull();

      setStatusFilter("agreementGenerated", true);
      expect(filters.value.agreementGenerated).toBe(true);

      setStatusFilter("agreementGenerated", false);
      expect(filters.value.agreementGenerated).toBe(false);

      setStatusFilter("agreementGenerated", null);
      expect(filters.value.agreementGenerated).toBeNull();
    });

    it("should update activeFilterCount when status filters are set (non-null)", () => {
      const { activeFilterCount, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      // No modified filters initially
      expect(activeFilterCount.value).toBe(0);

      // Setting to true counts
      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(1);

      // Setting to false also counts (it's a filter!)
      setStatusFilter("paid", false);
      expect(activeFilterCount.value).toBe(2);

      // Setting another to true should count
      setStatusFilter("agreementSigned", true);
      expect(activeFilterCount.value).toBe(3);

      // Setting back to null removes from count
      setStatusFilter("validated", null);
      expect(activeFilterCount.value).toBe(2);
    });

    it("should update queryParams for each status filter", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("validated", true);
      // declined (default) and validated are included
      expect(queryParams.value).toEqual({
        "filter[validated]": true,
        "filter[declined]": false,
      });

      setStatusFilter("paid", false);
      expect(queryParams.value).toEqual({
        "filter[validated]": true,
        "filter[paid]": false,
        "filter[declined]": false,
      });
    });

    it("should handle all status filter types", () => {
      const { filters, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("validated", true);
      setStatusFilter("paid", false);
      setStatusFilter("agreementGenerated", true);
      setStatusFilter("agreementSigned", false);
      setStatusFilter("suggestion", true);

      expect(filters.value.validated).toBe(true);
      expect(filters.value.paid).toBe(false);
      expect(filters.value.agreementGenerated).toBe(true);
      expect(filters.value.agreementSigned).toBe(false);
      expect(filters.value.suggestion).toBe(true);
    });
  });

  describe("clearFilter", () => {
    it("should clear a specific pack filter", () => {
      const { filters, setPackFilter, clearFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-789");
      expect(filters.value.packId).toBe("pack-789");

      clearFilter("packId");
      expect(filters.value.packId).toBeNull();
    });

    it("should clear a specific status filter (reset to null)", () => {
      const { filters, setStatusFilter, clearFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("validated", true);
      expect(filters.value.validated).toBe(true);

      clearFilter("validated");
      // Boolean filters reset to null (default)
      expect(filters.value.validated).toBeNull();
    });

    it("should update activeFilterCount when clearing filters", () => {
      const { activeFilterCount, setPackFilter, setStatusFilter, clearFilter } =
        useSponsorFilters({
          orgSlug,
          eventSlug,
        });

      setPackFilter("pack-123");
      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(2);

      clearFilter("packId");
      expect(activeFilterCount.value).toBe(1);

      clearFilter("validated");
      // validated is now null (default), so count is 0
      expect(activeFilterCount.value).toBe(0);
    });
  });

  describe("clearAllFilters", () => {
    it("should reset all filters to initial state", () => {
      const { filters, setPackFilter, setStatusFilter, clearAllFilters } =
        useSponsorFilters({
          orgSlug,
          eventSlug,
        });

      setPackFilter("pack-123");
      setStatusFilter("validated", true);
      setStatusFilter("paid", false);

      clearAllFilters();

      expect(filters.value).toEqual(initialFilterState);
    });

    it("should reset activeFilterCount to 0", () => {
      const {
        activeFilterCount,
        setPackFilter,
        setStatusFilter,
        clearAllFilters,
      } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-123");
      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(2); // packId + validated

      clearAllFilters();
      expect(activeFilterCount.value).toBe(0); // all back to defaults
    });

    it("should set isEmpty to true after clearing all", () => {
      const { isEmpty, setPackFilter, clearAllFilters } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");
      expect(isEmpty.value).toBe(false);

      clearAllFilters();
      expect(isEmpty.value).toBe(true); // all back to defaults
    });

    it("should reset queryParams to only declined filter", () => {
      const { queryParams, setPackFilter, setStatusFilter, clearAllFilters } =
        useSponsorFilters({
          orgSlug,
          eventSlug,
        });

      setPackFilter("pack-123");
      setStatusFilter("validated", true);
      expect(queryParams.value["filter[pack_id]"]).toBe("pack-123");
      expect(queryParams.value["filter[validated]"]).toBe(true);

      clearAllFilters();
      // All filters are reset to defaults, only declined remains
      expect(queryParams.value).toEqual({
        "filter[declined]": false,
      });
    });
  });

  describe("activeFilterCount", () => {
    it("should count only non-null filter values", () => {
      const { activeFilterCount, setPackFilter, setStatusFilter } =
        useSponsorFilters({
          orgSlug,
          eventSlug,
        });

      expect(activeFilterCount.value).toBe(0);

      setPackFilter("pack-123");
      expect(activeFilterCount.value).toBe(1);

      // true is non-null, so it counts
      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(2);

      // false is also non-null, so it counts too
      setStatusFilter("paid", false);
      expect(activeFilterCount.value).toBe(3);

      // Setting back to null removes from count
      setStatusFilter("paid", null);
      expect(activeFilterCount.value).toBe(2);
    });
  });

  describe("isEmpty", () => {
    it("should be true when no filters are active", () => {
      const { isEmpty } = useSponsorFilters({ orgSlug, eventSlug });

      expect(isEmpty.value).toBe(true);
    });

    it("should be false when at least one filter is active", () => {
      const { isEmpty, setPackFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");
      expect(isEmpty.value).toBe(false);
    });

    it("should be true after clearing all filters", () => {
      const { isEmpty, setPackFilter, clearAllFilters } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");
      expect(isEmpty.value).toBe(false);

      clearAllFilters();
      expect(isEmpty.value).toBe(true); // all back to defaults
    });
  });

  describe("queryParams", () => {
    it("should map packId to filter[pack_id]", () => {
      const { queryParams, setPackFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-abc");

      expect(queryParams.value["filter[pack_id]"]).toBe("pack-abc");
    });

    it("should map validated to filter[validated]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("validated", true);

      expect(queryParams.value["filter[validated]"]).toBe(true);
    });

    it("should map paid to filter[paid]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("paid", false);

      expect(queryParams.value["filter[paid]"]).toBe(false);
    });

    it("should map agreementGenerated to filter[agreement-generated]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("agreementGenerated", true);

      expect(queryParams.value["filter[agreement-generated]"]).toBe(true);
    });

    it("should map agreementSigned to filter[agreement-signed]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("agreementSigned", false);

      expect(queryParams.value["filter[agreement-signed]"]).toBe(false);
    });

    it("should map suggestion to filter[suggestion]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setStatusFilter("suggestion", true);

      expect(queryParams.value["filter[suggestion]"]).toBe(true);
    });

    it("should include only non-null filters in queryParams", () => {
      const { queryParams, setPackFilter, setStatusFilter } = useSponsorFilters(
        {
          orgSlug,
          eventSlug,
        },
      );

      setPackFilter("pack-multi");
      setStatusFilter("validated", true);
      setStatusFilter("paid", false);

      // Only non-null filters are included (plus declined default)
      expect(queryParams.value).toEqual({
        "filter[pack_id]": "pack-multi",
        "filter[validated]": true,
        "filter[paid]": false,
        "filter[declined]": false,
      });
    });

    it("should exclude null filters from queryParams", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      // packId is null by default, so not included
      setStatusFilter("paid", true);

      expect(queryParams.value).toEqual({
        "filter[paid]": true,
        "filter[declined]": false,
      });
      // packId not included because it's null
      expect(queryParams.value["filter[pack_id]"]).toBeUndefined();
      // Other boolean filters not included because they're null
      expect(queryParams.value["filter[validated]"]).toBeUndefined();
    });
  });

  describe("sessionStorage persistence", () => {
    // Note: useQueryStates stores values individually with a prefix pattern:
    // `${storageKeyPrefix}:${key}` instead of a single JSON object
    const getStorageKey = (key: string) => `${storageKey}:${key}`;

    it("should save filters to sessionStorage when filters change", async () => {
      vi.useFakeTimers();
      const { setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-persistent");
      await nextTick();
      vi.advanceTimersByTime(100); // Wait for throttle

      const stored = sessionStorage.getItem(getStorageKey("packId"));
      expect(stored).toBe("pack-persistent");
      vi.useRealTimers();
    });

    it("should restore filters from sessionStorage on mount", () => {
      // Store individual values as useQueryStates expects
      sessionStorage.setItem(getStorageKey("packId"), "pack-restored");
      sessionStorage.setItem(getStorageKey("validated"), "true");
      sessionStorage.setItem(getStorageKey("paid"), "false");

      const { filters } = useSponsorFilters({ orgSlug, eventSlug });

      expect(filters.value.packId).toBe("pack-restored");
      expect(filters.value.validated).toBe(true);
      expect(filters.value.paid).toBe(false);
    });

    it("should clear sessionStorage when all filters are cleared", async () => {
      vi.useFakeTimers();
      const { setPackFilter, clearAllFilters } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-temp");
      await nextTick();
      vi.advanceTimersByTime(100);
      expect(sessionStorage.getItem(getStorageKey("packId"))).toBe("pack-temp");

      clearAllFilters();
      await nextTick();
      vi.advanceTimersByTime(100);
      expect(sessionStorage.getItem(getStorageKey("packId"))).toBeNull();
      vi.useRealTimers();
    });

    it("should clear sessionStorage when isEmpty becomes true", async () => {
      vi.useFakeTimers();
      const { setPackFilter, clearFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-temp");
      await nextTick();
      vi.advanceTimersByTime(100);
      expect(sessionStorage.getItem(getStorageKey("packId"))).toBe("pack-temp");

      clearFilter("packId");
      await nextTick();
      vi.advanceTimersByTime(100);
      expect(sessionStorage.getItem(getStorageKey("packId"))).toBeNull();
      vi.useRealTimers();
    });

    it("should handle invalid data in sessionStorage gracefully", () => {
      // Store a value that will be parsed but results in default
      sessionStorage.setItem(getStorageKey("packId"), "");

      const { filters } = useSponsorFilters({ orgSlug, eventSlug });

      // Should fall back to initial state (null for stringOrNull parser)
      expect(filters.value.packId).toBeNull();
    });

    it("should use event-specific storage key prefix", async () => {
      vi.useFakeTimers();
      const { setPackFilter: setPackFilter1 } = useSponsorFilters({
        orgSlug: "org1",
        eventSlug: "event1",
      });
      const { setPackFilter: setPackFilter2 } = useSponsorFilters({
        orgSlug: "org2",
        eventSlug: "event2",
      });

      setPackFilter1("pack-1");
      setPackFilter2("pack-2");
      await nextTick();
      vi.advanceTimersByTime(100);

      const stored1 = sessionStorage.getItem(
        "sponsor-filters:org1:event1:packId",
      );
      const stored2 = sessionStorage.getItem(
        "sponsor-filters:org2:event2:packId",
      );

      expect(stored1).toBe("pack-1");
      expect(stored2).toBe("pack-2");
      vi.useRealTimers();
    });

    it("should update sessionStorage reactively as filters change", async () => {
      vi.useFakeTimers();
      const { setPackFilter, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-1");
      await nextTick();
      vi.advanceTimersByTime(100);
      expect(sessionStorage.getItem(getStorageKey("packId"))).toBe("pack-1");

      setStatusFilter("validated", true);
      await nextTick();
      vi.advanceTimersByTime(100);
      expect(sessionStorage.getItem(getStorageKey("packId"))).toBe("pack-1");
      expect(sessionStorage.getItem(getStorageKey("validated"))).toBe("true");
      vi.useRealTimers();
    });
  });
});
