// front/stores/favorites.ts
import { defineStore } from "pinia";
import {
  getUsersMeFavoriteEvents,
  putUsersMeFavoriteEvent,
  deleteUsersMeFavoriteEvent,
  type EventSummary,
} from "~/utils/api";
import type { EntityState } from "~/types/generics";

interface FavoritesState extends EntityState<EventSummary> {
  loaded: boolean;
}

export const useFavoritesStore = defineStore("favorites", {
  state: (): FavoritesState => ({
    items: [],
    loading: false,
    error: null,
    loaded: false,
  }),

  getters: {
    /**
     * Set of favorited event slugs — O(1) `isFavorite` lookup.
     */
    slugSet: (state) => new Set(state.items.map((e) => e.slug)),

    /**
     * The next 5 events by start_time (the server already returns them in ascending order).
     */
    top5: (state) => state.items.slice(0, 5),
  },

  actions: {
    /**
     * O(1) check by slug.
     */
    isFavorite(eventSlug: string): boolean {
      return this.slugSet.has(eventSlug);
    },

    /**
     * Fetch the full list from the server and replace `items` in place.
     * Idempotent — safe to call multiple times.
     */
    async load() {
      this.loading = true;
      this.error = null;
      try {
        const response = await getUsersMeFavoriteEvents({ responseType: "json" });
        this.items = response.data as unknown as EventSummary[];
        this.loaded = true;
      } catch (e) {
        this.error = e instanceof Error ? e.message : String(e);
      } finally {
        this.loading = false;
      }
    },

    /**
     * Add a favorite optimistically. If `optimistic` is provided, push it to `items` immediately.
     * On HTTP 409 (already favorited): treat as success, no toast.
     * On any other error: rollback and toast.
     */
    async add(eventSlug: string, optimistic?: EventSummary) {
      const alreadyHad = this.isFavorite(eventSlug);
      if (optimistic && !alreadyHad) {
        this.items.push(optimistic);
      }
      try {
        await putUsersMeFavoriteEvent(eventSlug);
      } catch (e: unknown) {
        const status = (e as { response?: { status?: number } })?.response?.status;
        if (status === 409) {
          // Already favorited server-side. Our optimistic insert is correct. Silently succeed.
          return;
        }
        // Rollback the optimistic push (if any) and toast.
        if (optimistic && !alreadyHad) {
          this.items = this.items.filter((e) => e.slug !== eventSlug);
        }
        useCustomToast().error("Impossible d'ajouter ce favori. Veuillez réessayer.");
        throw e;
      }
    },

    /**
     * Remove a favorite optimistically. Splice from `items` immediately.
     * On HTTP 404 (already gone): treat as success, no toast.
     * On any other error: refetch (simpler than re-inserting at the right position) and toast.
     */
    async remove(eventSlug: string) {
      const removed = this.items.find((e) => e.slug === eventSlug);
      this.items = this.items.filter((e) => e.slug !== eventSlug);
      try {
        await deleteUsersMeFavoriteEvent(eventSlug);
      } catch (e: unknown) {
        const status = (e as { response?: { status?: number } })?.response?.status;
        if (status === 404) {
          // Already not favorited server-side. Our optimistic splice is correct. Silently succeed.
          return;
        }
        // Rollback the optimistic splice and toast.
        if (removed) {
          await this.load();
        }
        useCustomToast().error("Impossible de retirer ce favori. Veuillez réessayer.");
        throw e;
      }
    },

    /**
     * Reset everything (used by the logout flow if ever invoked).
     */
    reset() {
      this.items = [];
      this.loaded = false;
      this.loading = false;
      this.error = null;
    },
  },
});
