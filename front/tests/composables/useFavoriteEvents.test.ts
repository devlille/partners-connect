import { describe, it, expect, beforeEach, vi } from "vitest";
import { useFavoriteEvents, type FavoriteEvent } from "~/composables/useFavoriteEvents";

describe("useFavoriteEvents", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.restoreAllMocks();

    // Mock localStorage properly
    const localStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
      length: 0,
      key: vi.fn(),
    };

    vi.stubGlobal("localStorage", localStorageMock);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe("loadFavorites", () => {
    it("should load favorites from localStorage", () => {
      const mockFavorites: FavoriteEvent[] = [
        {
          orgSlug: "test-org",
          orgName: "Test Org",
          eventSlug: "test-event",
          eventName: "Test Event",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      vi.mocked(localStorage.getItem).mockReturnValue(JSON.stringify(mockFavorites));

      const { loadFavorites, favorites } = useFavoriteEvents();
      loadFavorites();

      expect(localStorage.getItem).toHaveBeenCalledWith("favorite_events");
      expect(favorites.value).toEqual(mockFavorites);
    });

    it("should handle invalid JSON in localStorage", () => {
      vi.mocked(localStorage.getItem).mockReturnValue("invalid json");
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

      const { loadFavorites, favorites } = useFavoriteEvents();
      loadFavorites();

      expect(consoleErrorSpy).toHaveBeenCalled();
      expect(favorites.value).toEqual([]);

      consoleErrorSpy.mockRestore();
    });

    it("should return empty array when localStorage is empty", () => {
      vi.mocked(localStorage.getItem).mockReturnValue(null);

      const { loadFavorites, favorites } = useFavoriteEvents();
      loadFavorites();

      expect(favorites.value).toEqual([]);
    });

    it("should reject invalid data with missing fields", () => {
      const invalidData = [
        {
          orgSlug: "test-org",
          // Missing orgName
          eventSlug: "test-event",
          eventName: "Test Event",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      vi.mocked(localStorage.getItem).mockReturnValue(JSON.stringify(invalidData));
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

      const { loadFavorites, favorites } = useFavoriteEvents();
      loadFavorites();

      expect(favorites.value).toEqual([]);
      expect(localStorage.removeItem).toHaveBeenCalledWith("favorite_events");
      consoleErrorSpy.mockRestore();
    });

    it("should reject data with invalid date format", () => {
      const invalidData = [
        {
          orgSlug: "test-org",
          orgName: "Test Org",
          eventSlug: "test-event",
          eventName: "Test Event",
          addedAt: "not-a-valid-date",
        },
      ];

      vi.mocked(localStorage.getItem).mockReturnValue(JSON.stringify(invalidData));
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

      const { loadFavorites, favorites } = useFavoriteEvents();
      loadFavorites();

      expect(favorites.value).toEqual([]);
      expect(localStorage.removeItem).toHaveBeenCalledWith("favorite_events");
      consoleErrorSpy.mockRestore();
    });

    it("should reject data exceeding string length limits", () => {
      const invalidData = [
        {
          orgSlug: "a".repeat(101), // Exceeds max 100
          orgName: "Test Org",
          eventSlug: "test-event",
          eventName: "Test Event",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      vi.mocked(localStorage.getItem).mockReturnValue(JSON.stringify(invalidData));
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

      const { loadFavorites, favorites } = useFavoriteEvents();
      loadFavorites();

      expect(favorites.value).toEqual([]);
      expect(localStorage.removeItem).toHaveBeenCalledWith("favorite_events");
      consoleErrorSpy.mockRestore();
    });

    it("should reject array exceeding maximum favorites limit", () => {
      const tooManyFavorites = Array.from({ length: 101 }, (_, i) => ({
        orgSlug: `org-${i}`,
        orgName: `Org ${i}`,
        eventSlug: `event-${i}`,
        eventName: `Event ${i}`,
        addedAt: "2025-01-01T00:00:00.000Z",
      }));

      vi.mocked(localStorage.getItem).mockReturnValue(JSON.stringify(tooManyFavorites));
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

      const { loadFavorites, favorites } = useFavoriteEvents();
      loadFavorites();

      expect(favorites.value).toEqual([]);
      expect(localStorage.removeItem).toHaveBeenCalledWith("favorite_events");
      consoleErrorSpy.mockRestore();
    });
  });

  describe("addFavorite", () => {
    it("should add a favorite event", () => {
      const { addFavorite, favorites } = useFavoriteEvents();

      addFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(favorites.value).toHaveLength(1);
      expect(favorites.value[0].orgSlug).toBe("test-org");
      expect(favorites.value[0].eventSlug).toBe("test-event");
      expect(favorites.value[0].addedAt).toBeDefined();
    });

    it("should not add duplicate favorites", () => {
      const { addFavorite, favorites } = useFavoriteEvents();

      const event = {
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      };

      addFavorite(event);
      addFavorite(event);

      expect(favorites.value).toHaveLength(1);
    });

    it("should save to localStorage when adding a favorite", () => {
      const { addFavorite } = useFavoriteEvents();

      addFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(localStorage.setItem).toHaveBeenCalledWith("favorite_events", expect.any(String));
    });

    it("should return false when adding a duplicate", () => {
      const { addFavorite } = useFavoriteEvents();

      const event = {
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      };

      const firstAdd = addFavorite(event);
      const secondAdd = addFavorite(event);

      expect(firstAdd).toBe(true);
      expect(secondAdd).toBe(false);
    });

    it("should prevent adding more than 100 favorites", () => {
      const { addFavorite, favorites } = useFavoriteEvents();
      const consoleWarnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

      // Add 100 favorites
      for (let i = 0; i < 100; i++) {
        addFavorite({
          orgSlug: `org-${i}`,
          orgName: `Org ${i}`,
          eventSlug: `event-${i}`,
          eventName: `Event ${i}`,
        });
      }

      expect(favorites.value).toHaveLength(100);

      // Try to add 101st favorite
      const result = addFavorite({
        orgSlug: "org-101",
        orgName: "Org 101",
        eventSlug: "event-101",
        eventName: "Event 101",
      });

      expect(result).toBe(false);
      expect(favorites.value).toHaveLength(100);
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        "Cannot add favorite: maximum limit of 100 reached",
      );

      consoleWarnSpy.mockRestore();
    });

    it("should reject invalid data when adding favorite", () => {
      const { addFavorite, favorites } = useFavoriteEvents();
      const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

      const result = addFavorite({
        orgSlug: "a".repeat(101), // Exceeds max length
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(result).toBe(false);
      expect(favorites.value).toHaveLength(0);
      expect(consoleErrorSpy).toHaveBeenCalled();

      consoleErrorSpy.mockRestore();
    });
  });

  describe("removeFavorite", () => {
    it("should remove a favorite event", () => {
      const { addFavorite, removeFavorite, favorites } = useFavoriteEvents();

      addFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(favorites.value).toHaveLength(1);

      removeFavorite("test-org", "test-event");

      expect(favorites.value).toHaveLength(0);
    });

    it("should save to localStorage when removing a favorite", () => {
      const { addFavorite, removeFavorite } = useFavoriteEvents();

      addFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      vi.mocked(localStorage.setItem).mockClear();

      removeFavorite("test-org", "test-event");

      expect(localStorage.setItem).toHaveBeenCalledWith("favorite_events", expect.any(String));
    });

    it("should not throw error when removing non-existent favorite", () => {
      const { removeFavorite } = useFavoriteEvents();

      expect(() => {
        removeFavorite("non-existent-org", "non-existent-event");
      }).not.toThrow();
    });
  });

  describe("isFavorite", () => {
    it("should return true for favorited events", () => {
      const { addFavorite, isFavorite } = useFavoriteEvents();

      addFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(isFavorite("test-org", "test-event")).toBe(true);
    });

    it("should return false for non-favorited events", () => {
      const { isFavorite } = useFavoriteEvents();

      expect(isFavorite("test-org", "test-event")).toBe(false);
    });
  });

  describe("toggleFavorite", () => {
    it("should add favorite when not already favorited", () => {
      const { toggleFavorite, isFavorite } = useFavoriteEvents();

      expect(isFavorite("test-org", "test-event")).toBe(false);

      toggleFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(isFavorite("test-org", "test-event")).toBe(true);
    });

    it("should remove favorite when already favorited", () => {
      const { toggleFavorite, isFavorite } = useFavoriteEvents();

      toggleFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(isFavorite("test-org", "test-event")).toBe(true);

      toggleFavorite({
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
      });

      expect(isFavorite("test-org", "test-event")).toBe(false);
    });
  });

  describe("getFavorites", () => {
    it("should return all favorites sorted by date (most recent first)", () => {
      const { addFavorite, getFavorites } = useFavoriteEvents();

      addFavorite({
        orgSlug: "org1",
        orgName: "Org 1",
        eventSlug: "event1",
        eventName: "Event 1",
      });

      // Wait a bit to ensure different timestamps
      vi.useFakeTimers();
      vi.advanceTimersByTime(100);

      addFavorite({
        orgSlug: "org2",
        orgName: "Org 2",
        eventSlug: "event2",
        eventName: "Event 2",
      });

      vi.useRealTimers();

      const allFavorites = getFavorites();

      expect(allFavorites).toHaveLength(2);
      expect(allFavorites[0].eventSlug).toBe("event2"); // Most recent first
      expect(allFavorites[1].eventSlug).toBe("event1");
    });

    it("should return empty array when no favorites exist", () => {
      const { getFavorites } = useFavoriteEvents();

      expect(getFavorites()).toEqual([]);
    });
  });
});
