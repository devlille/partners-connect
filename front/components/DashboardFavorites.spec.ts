import { describe, it, expect, beforeEach, vi } from "vitest";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import DashboardFavorites from "./DashboardFavorites.vue";
import { useFavoritesStore } from "~/stores/favorites";
import type { EventSummary } from "~/utils/api";

// Mock the API so the store's lazy-load (triggered by the composable) doesn't fire
// a real HTTP call on mount.
vi.mock("~/utils/api", () => ({
  getUsersMeFavoriteEvents: vi.fn().mockResolvedValue({ data: [] }),
  putUsersMeFavoriteEvent: vi.fn(),
  deleteUsersMeFavoriteEvent: vi.fn(),
}));

function makeEvent(slug: string, name: string = `Event ${slug}`): EventSummary {
  return {
    slug,
    name,
    start_time: "2030-06-01T00:00:00",
    end_time: "2030-06-02T00:00:00",
    submission_start_time: "2030-05-01T00:00:00",
    submission_end_time: "2030-05-31T00:00:00",
    org_slug: "org-a",
    org_name: "Org A",
  };
}

describe("DashboardFavorites", () => {
  beforeEach(() => {
    // mountSuspended uses the Nuxt-app-level Pinia (set up by @pinia/nuxt).
    // Reset the store via its own reset() action so per-test state doesn't leak.
    useFavoritesStore().reset();
  });

  describe("visibility based on route", () => {
    it("renders nothing on /orgs/{slug}/events/{eventSlug}", async () => {
      const store = useFavoritesStore();
      store.items = [makeEvent("a")];
      store.loaded = true;

      const wrapper = await mountSuspended(DashboardFavorites, {
        route: "/orgs/org-a/events/event-a",
      });

      expect(wrapper.find("nav").exists()).toBe(false);
      expect(wrapper.find("h3").exists()).toBe(false);
    });

    it("renders on /orgs", async () => {
      const store = useFavoritesStore();
      store.items = [makeEvent("a")];
      store.loaded = true;

      const wrapper = await mountSuspended(DashboardFavorites, {
        route: "/orgs",
      });

      expect(wrapper.find("h3").text()).toBe("Mes favoris");
    });

    it("renders on /orgs/foo", async () => {
      const store = useFavoritesStore();
      store.items = [makeEvent("a")];
      store.loaded = true;

      const wrapper = await mountSuspended(DashboardFavorites, {
        route: "/orgs/foo",
      });

      expect(wrapper.find("h3").exists()).toBe(true);
    });

    it("renders on /orgs/foo/users (non-events sub-route)", async () => {
      const store = useFavoritesStore();
      store.items = [makeEvent("a")];
      store.loaded = true;

      const wrapper = await mountSuspended(DashboardFavorites, {
        route: "/orgs/foo/users",
      });

      expect(wrapper.find("h3").exists()).toBe(true);
    });
  });

  describe("content rendering", () => {
    it("hides entirely when store has 0 favorites", async () => {
      const store = useFavoritesStore();
      store.items = [];
      store.loaded = true;

      const wrapper = await mountSuspended(DashboardFavorites, {
        route: "/orgs",
      });

      expect(wrapper.find("h3").exists()).toBe(false);
      expect(wrapper.find("nav").exists()).toBe(false);
    });

    it("renders up to 5 entries", async () => {
      const store = useFavoritesStore();
      store.items = ["a", "b", "c", "d", "e", "f", "g"].map((s) => makeEvent(s));
      store.loaded = true;

      const wrapper = await mountSuspended(DashboardFavorites, {
        route: "/orgs",
      });

      expect(wrapper.findAll("a")).toHaveLength(5);
    });

    it("each entry links to /orgs/{org_slug}/events/{slug}", async () => {
      const store = useFavoritesStore();
      store.items = [makeEvent("event-1", "First")];
      store.loaded = true;

      const wrapper = await mountSuspended(DashboardFavorites, {
        route: "/orgs",
      });

      const link = wrapper.find("a");
      expect(link.attributes("href")).toBe("/orgs/org-a/events/event-1");
      expect(link.text()).toContain("First");
    });
  });
});
