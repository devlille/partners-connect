import { describe, it, expect, beforeEach, vi } from "vitest";
import { setActivePinia, createPinia } from "pinia";

// Mock the API functions before importing the store.
vi.mock("~/utils/api", () => ({
  getUsersMeFavoriteEvents: vi.fn(),
  putUsersMeFavoriteEvent: vi.fn(),
  deleteUsersMeFavoriteEvent: vi.fn(),
}));

// Mock the toast composable. Vitest with environment:"nuxt" resolves Nuxt
// auto-imports through real ESM, so mocking the source file is what intercepts
// the call site inside the store action.
const toastErrorMock = vi.fn();
vi.mock("~/composables/useCustomToast", () => ({
  useCustomToast: () => ({
    error: toastErrorMock,
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
    addToast: vi.fn(),
    removeToast: vi.fn(),
  }),
  ToastType: { SUCCESS: "success", ERROR: "error", WARNING: "warning", INFO: "info" },
}));

import {
  getUsersMeFavoriteEvents,
  putUsersMeFavoriteEvent,
  deleteUsersMeFavoriteEvent,
  type EventSummary,
} from "~/utils/api";
import { useFavoritesStore } from "./favorites";

function makeEvent(slug: string, startTime: string = "2030-06-01T00:00:00"): EventSummary {
  return {
    slug,
    name: `Event ${slug}`,
    start_time: startTime,
    end_time: "2030-06-02T00:00:00",
    submission_start_time: "2030-05-01T00:00:00",
    submission_end_time: "2030-05-31T00:00:00",
    org_slug: "org-a",
    org_name: "Org A",
  };
}

describe("useFavoritesStore", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    toastErrorMock.mockClear();
  });

  describe("load()", () => {
    it("populates items from the server response", async () => {
      const event1 = makeEvent("a");
      const event2 = makeEvent("b");
      vi.mocked(getUsersMeFavoriteEvents).mockResolvedValue({ data: [event1, event2] } as never);

      const store = useFavoritesStore();
      await store.load();

      expect(store.items).toEqual([event1, event2]);
      expect(store.loaded).toBe(true);
      expect(store.error).toBe(null);
    });

    it("sets error on network failure", async () => {
      vi.mocked(getUsersMeFavoriteEvents).mockRejectedValue(new Error("network down"));

      const store = useFavoritesStore();
      await store.load();

      expect(store.items).toEqual([]);
      expect(store.error).toBe("network down");
      expect(store.loaded).toBe(false);
    });
  });

  describe("isFavorite()", () => {
    it("returns true for items in the store", () => {
      const store = useFavoritesStore();
      store.items = [makeEvent("a")];
      expect(store.isFavorite("a")).toBe(true);
    });

    it("returns false for items not in the store", () => {
      const store = useFavoritesStore();
      store.items = [makeEvent("a")];
      expect(store.isFavorite("b")).toBe(false);
    });
  });

  describe("add()", () => {
    it("optimistically inserts and calls PUT on success", async () => {
      vi.mocked(putUsersMeFavoriteEvent).mockResolvedValue({ status: 201 } as never);

      const store = useFavoritesStore();
      const event = makeEvent("a");
      await store.add("a", event);

      expect(store.items).toEqual([event]);
      expect(putUsersMeFavoriteEvent).toHaveBeenCalledWith("a");
      expect(toastErrorMock).not.toHaveBeenCalled();
    });

    it("treats HTTP 409 as a silent success", async () => {
      const error = { response: { status: 409 } };
      vi.mocked(putUsersMeFavoriteEvent).mockRejectedValue(error);

      const store = useFavoritesStore();
      const event = makeEvent("a");
      await store.add("a", event);

      expect(store.items).toEqual([event]);
      expect(toastErrorMock).not.toHaveBeenCalled();
    });

    it("rolls back the optimistic insert and toasts on other errors", async () => {
      const error = { response: { status: 500 } };
      vi.mocked(putUsersMeFavoriteEvent).mockRejectedValue(error);

      const store = useFavoritesStore();
      const event = makeEvent("a");
      await expect(store.add("a", event)).rejects.toBe(error);

      expect(store.items).toEqual([]);
      expect(toastErrorMock).toHaveBeenCalledWith(
        "Impossible d'ajouter ce favori. Veuillez réessayer.",
      );
    });
  });

  describe("remove()", () => {
    it("optimistically splices and calls DELETE on success", async () => {
      vi.mocked(deleteUsersMeFavoriteEvent).mockResolvedValue({ status: 204 } as never);

      const store = useFavoritesStore();
      store.items = [makeEvent("a"), makeEvent("b")];
      await store.remove("a");

      expect(store.items.map((e) => e.slug)).toEqual(["b"]);
      expect(deleteUsersMeFavoriteEvent).toHaveBeenCalledWith("a");
      expect(toastErrorMock).not.toHaveBeenCalled();
    });

    it("treats HTTP 404 as a silent success", async () => {
      const error = { response: { status: 404 } };
      vi.mocked(deleteUsersMeFavoriteEvent).mockRejectedValue(error);

      const store = useFavoritesStore();
      store.items = [makeEvent("a")];
      await store.remove("a");

      expect(store.items).toEqual([]);
      expect(toastErrorMock).not.toHaveBeenCalled();
    });

    it("refetches and toasts on other errors", async () => {
      const error = { response: { status: 500 } };
      vi.mocked(deleteUsersMeFavoriteEvent).mockRejectedValue(error);
      const fresh = [makeEvent("a"), makeEvent("c")];
      vi.mocked(getUsersMeFavoriteEvents).mockResolvedValue({ data: fresh } as never);

      const store = useFavoritesStore();
      store.items = [makeEvent("a"), makeEvent("b")];
      await expect(store.remove("a")).rejects.toBe(error);

      expect(getUsersMeFavoriteEvents).toHaveBeenCalled();
      expect(store.items).toEqual(fresh);
      expect(toastErrorMock).toHaveBeenCalledWith(
        "Impossible de retirer ce favori. Veuillez réessayer.",
      );
    });
  });

  describe("top5 getter", () => {
    it("returns the first 5 items", () => {
      const store = useFavoritesStore();
      store.items = ["a", "b", "c", "d", "e", "f", "g"].map((s) => makeEvent(s));
      expect(store.top5.map((e) => e.slug)).toEqual(["a", "b", "c", "d", "e"]);
    });

    it("returns all items when fewer than 5", () => {
      const store = useFavoritesStore();
      store.items = ["a", "b"].map((s) => makeEvent(s));
      expect(store.top5.map((e) => e.slug)).toEqual(["a", "b"]);
    });
  });
});
