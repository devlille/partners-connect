import { describe, it, expect, beforeEach, vi } from "vitest";
import { setActivePinia, createPinia } from "pinia";
import { useBoothPlanStore } from "~/stores/boothPlan";

vi.stubGlobal("localStorage", {
  getItem: vi.fn(() => null),
  setItem: vi.fn(),
});

describe("useBoothPlanStore", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe("initial state", () => {
    it("should have empty urls map", () => {
      const store = useBoothPlanStore();
      expect(store.urls).toEqual({});
    });
  });

  describe("setUrl", () => {
    it("should store url for a given event slug", () => {
      const store = useBoothPlanStore();
      store.setUrl("devlille-2025", "https://example.com/booth-plan.png");

      expect(store.urls["devlille-2025"]).toBe("https://example.com/booth-plan.png");
    });

    it("should overwrite url for the same event slug", () => {
      const store = useBoothPlanStore();
      store.setUrl("devlille-2025", "https://example.com/booth-plan-v1.png");
      store.setUrl("devlille-2025", "https://example.com/booth-plan-v2.png");

      expect(store.urls["devlille-2025"]).toBe("https://example.com/booth-plan-v2.png");
    });

    it("should store urls for multiple events independently", () => {
      const store = useBoothPlanStore();
      store.setUrl("devlille-2025", "https://example.com/plan-2025.png");
      store.setUrl("devlille-2026", "https://example.com/plan-2026.png");

      expect(store.urls["devlille-2025"]).toBe("https://example.com/plan-2025.png");
      expect(store.urls["devlille-2026"]).toBe("https://example.com/plan-2026.png");
    });
  });

  describe("getUrl getter", () => {
    it("should return null for unknown event slug", () => {
      const store = useBoothPlanStore();
      expect(store.getUrl("unknown-event")).toBeNull();
    });

    it("should return url for known event slug", () => {
      const store = useBoothPlanStore();
      store.setUrl("devlille-2025", "https://example.com/booth-plan.png");

      expect(store.getUrl("devlille-2025")).toBe("https://example.com/booth-plan.png");
    });
  });
});
