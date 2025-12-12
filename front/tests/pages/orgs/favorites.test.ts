import { describe, it, expect } from "vitest";

describe("Favorites Page - Helper Functions", () => {
  describe("Date Formatting", () => {
    // Fonction pour formater les dates comme dans la page favorites
    function formatDate(isoDate: string): string {
      const date = new Date(isoDate);
      return date.toLocaleDateString("fr-FR", {
        year: "numeric",
        month: "long",
        day: "numeric",
      });
    }

    it("should format ISO date to French locale", () => {
      const isoDate = "2025-01-15T10:30:00.000Z";
      const formatted = formatDate(isoDate);

      expect(formatted).toContain("15");
      expect(formatted).toContain("janvier");
      expect(formatted).toContain("2025");
    });

    it("should handle different months", () => {
      const dates = [
        { iso: "2025-01-01T00:00:00.000Z", month: "janvier" },
        { iso: "2025-02-01T00:00:00.000Z", month: "février" },
        { iso: "2025-03-01T00:00:00.000Z", month: "mars" },
        { iso: "2025-12-01T00:00:00.000Z", month: "décembre" },
      ];

      dates.forEach(({ iso, month }) => {
        const formatted = formatDate(iso);
        expect(formatted).toContain(month);
      });
    });

    it("should handle leap year dates", () => {
      const leapDate = "2024-02-29T00:00:00.000Z";
      const formatted = formatDate(leapDate);

      expect(formatted).toContain("29");
      expect(formatted).toContain("février");
      expect(formatted).toContain("2024");
    });
  });

  describe("Favorite Event Filtering", () => {
    interface FavoriteEvent {
      orgSlug: string;
      orgName: string;
      eventSlug: string;
      eventName: string;
      addedAt: string;
    }

    it("should filter favorites by organization", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "Organization 1",
          eventSlug: "event1",
          eventName: "Event 1",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
        {
          orgSlug: "org2",
          orgName: "Organization 2",
          eventSlug: "event2",
          eventName: "Event 2",
          addedAt: "2025-01-02T00:00:00.000Z",
        },
        {
          orgSlug: "org1",
          orgName: "Organization 1",
          eventSlug: "event3",
          eventName: "Event 3",
          addedAt: "2025-01-03T00:00:00.000Z",
        },
      ];

      const org1Favorites = favorites.filter((fav) => fav.orgSlug === "org1");

      expect(org1Favorites).toHaveLength(2);
      expect(org1Favorites[0].eventSlug).toBe("event1");
      expect(org1Favorites[1].eventSlug).toBe("event3");
    });

    it("should sort favorites by date (most recent first)", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "Organization 1",
          eventSlug: "event1",
          eventName: "Event 1",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
        {
          orgSlug: "org2",
          orgName: "Organization 2",
          eventSlug: "event2",
          eventName: "Event 2",
          addedAt: "2025-01-03T00:00:00.000Z",
        },
        {
          orgSlug: "org3",
          orgName: "Organization 3",
          eventSlug: "event3",
          eventName: "Event 3",
          addedAt: "2025-01-02T00:00:00.000Z",
        },
      ];

      const sorted = [...favorites].sort(
        (a, b) => new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime(),
      );

      expect(sorted[0].eventSlug).toBe("event2"); // Most recent
      expect(sorted[1].eventSlug).toBe("event3");
      expect(sorted[2].eventSlug).toBe("event1"); // Oldest
    });

    it("should handle empty favorites list", () => {
      const favorites: FavoriteEvent[] = [];

      expect(favorites).toHaveLength(0);
      expect(favorites.filter((fav) => fav.orgSlug === "org1")).toHaveLength(0);
    });
  });

  describe("Navigation URL Generation", () => {
    function generateEventUrl(orgSlug: string, eventSlug: string): string {
      return `/orgs/${orgSlug}/events/${eventSlug}`;
    }

    it("should generate correct event URL", () => {
      const url = generateEventUrl("test-org", "test-event");
      expect(url).toBe("/orgs/test-org/events/test-event");
    });

    it("should handle URL-safe slugs", () => {
      const url = generateEventUrl("my-org-2025", "my-event-2025");
      expect(url).toBe("/orgs/my-org-2025/events/my-event-2025");
    });

    it("should handle slugs with numbers", () => {
      const url = generateEventUrl("devlille", "devlille-2026");
      expect(url).toBe("/orgs/devlille/events/devlille-2026");
    });
  });

  describe("Favorite Event Validation", () => {
    interface FavoriteEvent {
      orgSlug: string;
      orgName: string;
      eventSlug: string;
      eventName: string;
      addedAt: string;
    }

    function isValidFavorite(favorite: Partial<FavoriteEvent>): boolean {
      return !!(
        favorite.orgSlug &&
        favorite.orgName &&
        favorite.eventSlug &&
        favorite.eventName &&
        favorite.addedAt
      );
    }

    it("should validate complete favorite event", () => {
      const favorite: FavoriteEvent = {
        orgSlug: "test-org",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
        addedAt: "2025-01-01T00:00:00.000Z",
      };

      expect(isValidFavorite(favorite)).toBe(true);
    });

    it("should invalidate favorite missing orgSlug", () => {
      const favorite: Partial<FavoriteEvent> = {
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
        addedAt: "2025-01-01T00:00:00.000Z",
      };

      expect(isValidFavorite(favorite)).toBe(false);
    });

    it("should invalidate favorite missing eventSlug", () => {
      const favorite: Partial<FavoriteEvent> = {
        orgSlug: "test-org",
        orgName: "Test Org",
        eventName: "Test Event",
        addedAt: "2025-01-01T00:00:00.000Z",
      };

      expect(isValidFavorite(favorite)).toBe(false);
    });

    it("should invalidate favorite with empty strings", () => {
      const favorite: FavoriteEvent = {
        orgSlug: "",
        orgName: "Test Org",
        eventSlug: "test-event",
        eventName: "Test Event",
        addedAt: "2025-01-01T00:00:00.000Z",
      };

      expect(isValidFavorite(favorite)).toBe(false);
    });
  });

  describe("Duplicate Detection", () => {
    interface FavoriteEvent {
      orgSlug: string;
      orgName: string;
      eventSlug: string;
      eventName: string;
      addedAt: string;
    }

    function isDuplicate(favorites: FavoriteEvent[], orgSlug: string, eventSlug: string): boolean {
      return favorites.some((fav) => fav.orgSlug === orgSlug && fav.eventSlug === eventSlug);
    }

    it("should detect duplicate favorites", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "Organization 1",
          eventSlug: "event1",
          eventName: "Event 1",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      expect(isDuplicate(favorites, "org1", "event1")).toBe(true);
    });

    it("should not detect non-existent favorites", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "Organization 1",
          eventSlug: "event1",
          eventName: "Event 1",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      expect(isDuplicate(favorites, "org2", "event2")).toBe(false);
    });

    it("should handle partial slug matches", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "Organization 1",
          eventSlug: "event1",
          eventName: "Event 1",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      // Same event slug but different org
      expect(isDuplicate(favorites, "org2", "event1")).toBe(false);

      // Same org slug but different event
      expect(isDuplicate(favorites, "org1", "event2")).toBe(false);
    });
  });

  describe("Search and Filter", () => {
    interface FavoriteEvent {
      orgSlug: string;
      orgName: string;
      eventSlug: string;
      eventName: string;
      addedAt: string;
    }

    function searchFavorites(favorites: FavoriteEvent[], searchTerm: string): FavoriteEvent[] {
      const lowerSearch = searchTerm.toLowerCase();
      return favorites.filter(
        (fav) =>
          fav.eventName.toLowerCase().includes(lowerSearch) ||
          fav.orgName.toLowerCase().includes(lowerSearch),
      );
    }

    it("should search by event name", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "DevLille",
          eventSlug: "event1",
          eventName: "DevLille 2025",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
        {
          orgSlug: "org2",
          orgName: "TechConf",
          eventSlug: "event2",
          eventName: "TechConf 2025",
          addedAt: "2025-01-02T00:00:00.000Z",
        },
      ];

      const results = searchFavorites(favorites, "DevLille");

      expect(results).toHaveLength(1);
      expect(results[0].eventName).toBe("DevLille 2025");
    });

    it("should search by organization name", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "DevLille",
          eventSlug: "event1",
          eventName: "Event 2025",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
        {
          orgSlug: "org2",
          orgName: "TechConf",
          eventSlug: "event2",
          eventName: "Event 2025",
          addedAt: "2025-01-02T00:00:00.000Z",
        },
      ];

      const results = searchFavorites(favorites, "TechConf");

      expect(results).toHaveLength(1);
      expect(results[0].orgName).toBe("TechConf");
    });

    it("should be case insensitive", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "DevLille",
          eventSlug: "event1",
          eventName: "DevLille 2025",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      const results = searchFavorites(favorites, "devlille");

      expect(results).toHaveLength(1);
    });

    it("should return empty array when no matches", () => {
      const favorites: FavoriteEvent[] = [
        {
          orgSlug: "org1",
          orgName: "DevLille",
          eventSlug: "event1",
          eventName: "DevLille 2025",
          addedAt: "2025-01-01T00:00:00.000Z",
        },
      ];

      const results = searchFavorites(favorites, "nonexistent");

      expect(results).toHaveLength(0);
    });
  });
});
