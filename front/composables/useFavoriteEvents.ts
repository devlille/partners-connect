import { useFavoritesStore } from "~/stores/favorites";
import type { EventSummary } from "~/utils/api";

/**
 * Server-backed favorite events.
 *
 * The first call in a client-side session triggers a lazy fetch of the user's
 * favorites. Subsequent calls within the same session reuse the cached items
 * from the Pinia store. A full page reload re-hydrates.
 *
 * Mutations (`add`/`remove`/`toggle`) update the store optimistically and fire
 * the corresponding HTTP call in the background. HTTP 409 (PUT) and 404 (DELETE)
 * are treated as silent successes (race-condition convergence).
 */
export const useFavoriteEvents = () => {
  const store = useFavoritesStore();

  // Lazy hydrate on first read in a client-side context.
  if (import.meta.client && !store.loaded && !store.loading) {
    store.load();
  }

  return {
    favorites: computed<EventSummary[]>(() => store.items),
    top5: computed<EventSummary[]>(() => store.top5),
    isFavorite: (eventSlug: string): boolean => store.isFavorite(eventSlug),
    addFavorite: (eventSlug: string, optimistic?: EventSummary) =>
      store.add(eventSlug, optimistic),
    removeFavorite: (eventSlug: string) => store.remove(eventSlug),
    toggleFavorite: (eventSlug: string, optimistic?: EventSummary) =>
      store.isFavorite(eventSlug)
        ? store.remove(eventSlug)
        : store.add(eventSlug, optimistic),
  };
};
