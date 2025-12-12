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
    it("should initialize with all filters set to null", () => {
      const { filters } = useSponsorFilters({ orgSlug, eventSlug });

      expect(filters.value).toEqual(initialFilterState);
      expect(filters.value.packId).toBeNull();
      expect(filters.value.validated).toBeNull();
      expect(filters.value.paid).toBeNull();
      expect(filters.value.agreementGenerated).toBeNull();
      expect(filters.value.agreementSigned).toBeNull();
      expect(filters.value.suggestion).toBeNull();
    });

    it("should have activeFilterCount of 0 initially", () => {
      const { activeFilterCount } = useSponsorFilters({ orgSlug, eventSlug });

      expect(activeFilterCount.value).toBe(0);
    });

    it("should have isEmpty as true initially", () => {
      const { isEmpty } = useSponsorFilters({ orgSlug, eventSlug });

      expect(isEmpty.value).toBe(true);
    });

    it("should have empty queryParams initially", () => {
      const { queryParams } = useSponsorFilters({ orgSlug, eventSlug });

      expect(queryParams.value).toEqual({});
    });
  });

  describe("setPackFilter", () => {
    it("should set pack filter to specified value", () => {
      const { filters, setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-123");

      expect(filters.value.packId).toBe("pack-123");
    });

    it("should set pack filter to null when clearing", () => {
      const { filters, setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-123");
      expect(filters.value.packId).toBe("pack-123");

      setPackFilter(null);
      expect(filters.value.packId).toBeNull();
    });

    it("should update activeFilterCount when pack filter is set", () => {
      const { activeFilterCount, setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      expect(activeFilterCount.value).toBe(0);

      setPackFilter("pack-123");
      expect(activeFilterCount.value).toBe(1);
    });

    it("should update queryParams when pack filter is set", () => {
      const { queryParams, setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-456");

      expect(queryParams.value).toEqual({
        "filter[pack_id]": "pack-456",
      });
    });
  });

  describe("setStatusFilter", () => {
    it("should set validated filter to true", () => {
      const { filters, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("validated", true);

      expect(filters.value.validated).toBe(true);
    });

    it("should set paid filter to false", () => {
      const { filters, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("paid", false);

      expect(filters.value.paid).toBe(false);
    });

    it("should set agreementGenerated filter to null", () => {
      const { filters, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("agreementGenerated", true);
      expect(filters.value.agreementGenerated).toBe(true);

      setStatusFilter("agreementGenerated", null);
      expect(filters.value.agreementGenerated).toBeNull();
    });

    it("should update activeFilterCount when status filters are set", () => {
      const { activeFilterCount, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(1);

      setStatusFilter("paid", false);
      expect(activeFilterCount.value).toBe(2);

      setStatusFilter("agreementSigned", true);
      expect(activeFilterCount.value).toBe(3);
    });

    it("should update queryParams for each status filter", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("validated", true);
      expect(queryParams.value).toEqual({
        "filter[validated]": true,
      });

      setStatusFilter("paid", false);
      expect(queryParams.value).toEqual({
        "filter[validated]": true,
        "filter[paid]": false,
      });
    });

    it("should handle all status filter types", () => {
      const { filters, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

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
      const { filters, setPackFilter, clearFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-789");
      expect(filters.value.packId).toBe("pack-789");

      clearFilter("packId");
      expect(filters.value.packId).toBeNull();
    });

    it("should clear a specific status filter", () => {
      const { filters, setStatusFilter, clearFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("validated", true);
      expect(filters.value.validated).toBe(true);

      clearFilter("validated");
      expect(filters.value.validated).toBeNull();
    });

    it("should update activeFilterCount when clearing filters", () => {
      const { activeFilterCount, setPackFilter, setStatusFilter, clearFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");
      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(2);

      clearFilter("packId");
      expect(activeFilterCount.value).toBe(1);

      clearFilter("validated");
      expect(activeFilterCount.value).toBe(0);
    });
  });

  describe("clearAllFilters", () => {
    it("should reset all filters to initial state", () => {
      const { filters, setPackFilter, setStatusFilter, clearAllFilters } = useSponsorFilters({
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
      const { activeFilterCount, setPackFilter, setStatusFilter, clearAllFilters } =
        useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-123");
      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(2);

      clearAllFilters();
      expect(activeFilterCount.value).toBe(0);
    });

    it("should set isEmpty to true", () => {
      const { isEmpty, setPackFilter, clearAllFilters } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-123");
      expect(isEmpty.value).toBe(false);

      clearAllFilters();
      expect(isEmpty.value).toBe(true);
    });

    it("should clear queryParams", () => {
      const { queryParams, setPackFilter, setStatusFilter, clearAllFilters } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");
      setStatusFilter("validated", true);
      expect(Object.keys(queryParams.value).length).toBeGreaterThan(0);

      clearAllFilters();
      expect(queryParams.value).toEqual({});
    });
  });

  describe("activeFilterCount", () => {
    it("should count only non-null filter values", () => {
      const { activeFilterCount, setPackFilter, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      expect(activeFilterCount.value).toBe(0);

      setPackFilter("pack-123");
      expect(activeFilterCount.value).toBe(1);

      setStatusFilter("validated", true);
      expect(activeFilterCount.value).toBe(2);

      setStatusFilter("paid", false);
      expect(activeFilterCount.value).toBe(3);

      setStatusFilter("agreementGenerated", null);
      expect(activeFilterCount.value).toBe(3);
    });
  });

  describe("isEmpty", () => {
    it("should be true when no filters are active", () => {
      const { isEmpty } = useSponsorFilters({ orgSlug, eventSlug });

      expect(isEmpty.value).toBe(true);
    });

    it("should be false when at least one filter is active", () => {
      const { isEmpty, setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-123");
      expect(isEmpty.value).toBe(false);
    });

    it("should be true after clearing all filters", () => {
      const { isEmpty, setPackFilter, clearAllFilters } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-123");
      expect(isEmpty.value).toBe(false);

      clearAllFilters();
      expect(isEmpty.value).toBe(true);
    });
  });

  describe("queryParams", () => {
    it("should map packId to filter[pack_id]", () => {
      const { queryParams, setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-abc");

      expect(queryParams.value["filter[pack_id]"]).toBe("pack-abc");
    });

    it("should map validated to filter[validated]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("validated", true);

      expect(queryParams.value["filter[validated]"]).toBe(true);
    });

    it("should map paid to filter[paid]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("paid", false);

      expect(queryParams.value["filter[paid]"]).toBe(false);
    });

    it("should map agreementGenerated to filter[agreement-generated]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("agreementGenerated", true);

      expect(queryParams.value["filter[agreement-generated]"]).toBe(true);
    });

    it("should map agreementSigned to filter[agreement-signed]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("agreementSigned", false);

      expect(queryParams.value["filter[agreement-signed]"]).toBe(false);
    });

    it("should map suggestion to filter[suggestion]", () => {
      const { queryParams, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setStatusFilter("suggestion", true);

      expect(queryParams.value["filter[suggestion]"]).toBe(true);
    });

    it("should include all active filters in queryParams", () => {
      const { queryParams, setPackFilter, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-multi");
      setStatusFilter("validated", true);
      setStatusFilter("paid", false);

      expect(queryParams.value).toEqual({
        "filter[pack_id]": "pack-multi",
        "filter[validated]": true,
        "filter[paid]": false,
      });
    });

    it("should exclude null filters from queryParams", () => {
      const { queryParams, setPackFilter, setStatusFilter } = useSponsorFilters({
        orgSlug,
        eventSlug,
      });

      setPackFilter("pack-123");
      setStatusFilter("validated", null);
      setStatusFilter("paid", true);

      expect(queryParams.value).toEqual({
        "filter[pack_id]": "pack-123",
        "filter[paid]": true,
      });
      expect(queryParams.value["filter[validated]"]).toBeUndefined();
    });
  });

  describe("sessionStorage persistence", () => {
    it("should save filters to sessionStorage when filters change", async () => {
      const { setPackFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-persistent");
      await nextTick();

      const stored = sessionStorage.getItem(storageKey);
      expect(stored).not.toBeNull();

      const parsed = JSON.parse(stored!);
      expect(parsed.packId).toBe("pack-persistent");
    });

    it("should restore filters from sessionStorage on mount", () => {
      const savedState = {
        packId: "pack-restored",
        validated: true,
        paid: false,
        agreementGenerated: null,
        agreementSigned: null,
        suggestion: null,
      };

      sessionStorage.setItem(storageKey, JSON.stringify(savedState));

      const { filters } = useSponsorFilters({ orgSlug, eventSlug });

      expect(filters.value.packId).toBe("pack-restored");
      expect(filters.value.validated).toBe(true);
      expect(filters.value.paid).toBe(false);
    });

    it("should clear sessionStorage when all filters are cleared", async () => {
      const { setPackFilter, clearAllFilters } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-temp");
      await nextTick();
      expect(sessionStorage.getItem(storageKey)).not.toBeNull();

      clearAllFilters();
      await nextTick();
      expect(sessionStorage.getItem(storageKey)).toBeNull();
    });

    it("should clear sessionStorage when isEmpty becomes true", async () => {
      const { setPackFilter, clearFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-temp");
      await nextTick();
      expect(sessionStorage.getItem(storageKey)).not.toBeNull();

      clearFilter("packId");
      await nextTick();
      expect(sessionStorage.getItem(storageKey)).toBeNull();
    });

    it("should validate restored data with Zod and use initial state if invalid", () => {
      const invalidState = {
        packId: 123, // Should be string or null
        validated: "yes", // Should be boolean or null
        paid: null,
        agreementGenerated: null,
        agreementSigned: null,
        suggestion: null,
      };

      sessionStorage.setItem(storageKey, JSON.stringify(invalidState));

      const { filters } = useSponsorFilters({ orgSlug, eventSlug });

      // Should fall back to initial state
      expect(filters.value).toEqual(initialFilterState);
    });

    it("should handle malformed JSON in sessionStorage gracefully", () => {
      sessionStorage.setItem(storageKey, "{invalid json}");

      const { filters } = useSponsorFilters({ orgSlug, eventSlug });

      // Should fall back to initial state
      expect(filters.value).toEqual(initialFilterState);
    });

    it("should use event-specific storage key", async () => {
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

      const stored1 = sessionStorage.getItem("sponsor-filters:org1:event1");
      const stored2 = sessionStorage.getItem("sponsor-filters:org2:event2");

      expect(stored1).not.toBeNull();
      expect(stored2).not.toBeNull();
      expect(JSON.parse(stored1!).packId).toBe("pack-1");
      expect(JSON.parse(stored2!).packId).toBe("pack-2");
    });

    it("should update sessionStorage reactively as filters change", async () => {
      const { setPackFilter, setStatusFilter } = useSponsorFilters({ orgSlug, eventSlug });

      setPackFilter("pack-1");
      await nextTick();
      let stored = JSON.parse(sessionStorage.getItem(storageKey)!);
      expect(stored.packId).toBe("pack-1");

      setStatusFilter("validated", true);
      await nextTick();
      stored = JSON.parse(sessionStorage.getItem(storageKey)!);
      expect(stored.packId).toBe("pack-1");
      expect(stored.validated).toBe(true);
    });
  });
});
