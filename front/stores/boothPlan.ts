import { defineStore } from "pinia";

const STORAGE_KEY = "booth_plan_urls";

function loadFromStorage(): Record<string, string> {
  if (import.meta.client) {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? (JSON.parse(stored) as Record<string, string>) : {};
    } catch {
      return {};
    }
  }
  return {};
}

export const useBoothPlanStore = defineStore("boothPlan", {
  state: () => ({
    urls: loadFromStorage(),
  }),

  getters: {
    getUrl:
      (state) =>
      (eventSlug: string): string | null =>
        state.urls[eventSlug] ?? null,
  },

  actions: {
    setUrl(eventSlug: string, url: string) {
      this.urls[eventSlug] = url;
      if (import.meta.client) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(this.urls));
      }
    },
  },
});
