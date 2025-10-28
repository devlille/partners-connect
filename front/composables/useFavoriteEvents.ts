export interface FavoriteEvent {
  orgSlug: string;
  orgName: string;
  eventSlug: string;
  eventName: string;
  addedAt: string; // ISO date string
}

const STORAGE_KEY = 'favorite_events';

export const useFavoriteEvents = () => {
  const favorites = ref<FavoriteEvent[]>([]);

  /**
   * Charge les favoris depuis le localStorage
   */
  function loadFavorites() {
    if (import.meta.client) {
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          favorites.value = JSON.parse(stored);
        }
      } catch (error) {
        console.error('Failed to load favorite events:', error);
        favorites.value = [];
      }
    }
  }

  /**
   * Sauvegarde les favoris dans le localStorage
   */
  function saveFavorites() {
    if (import.meta.client) {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(favorites.value));
      } catch (error) {
        console.error('Failed to save favorite events:', error);
      }
    }
  }

  /**
   * Ajoute un événement aux favoris
   */
  function addFavorite(event: Omit<FavoriteEvent, 'addedAt'>) {
    const existing = favorites.value.find(
      f => f.orgSlug === event.orgSlug && f.eventSlug === event.eventSlug
    );

    if (!existing) {
      favorites.value.push({
        ...event,
        addedAt: new Date().toISOString()
      });
      saveFavorites();
    }
  }

  /**
   * Retire un événement des favoris
   */
  function removeFavorite(orgSlug: string, eventSlug: string) {
    favorites.value = favorites.value.filter(
      f => !(f.orgSlug === orgSlug && f.eventSlug === eventSlug)
    );
    saveFavorites();
  }

  /**
   * Vérifie si un événement est dans les favoris
   */
  function isFavorite(orgSlug: string, eventSlug: string): boolean {
    return favorites.value.some(
      f => f.orgSlug === orgSlug && f.eventSlug === eventSlug
    );
  }

  /**
   * Bascule le statut favori d'un événement
   */
  function toggleFavorite(event: Omit<FavoriteEvent, 'addedAt'>) {
    if (isFavorite(event.orgSlug, event.eventSlug)) {
      removeFavorite(event.orgSlug, event.eventSlug);
    } else {
      addFavorite(event);
    }
  }

  /**
   * Récupère tous les favoris triés par date d'ajout (plus récent en premier)
   */
  function getFavorites(): FavoriteEvent[] {
    return [...favorites.value].sort((a, b) =>
      new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime()
    );
  }

  // Charger les favoris au montage
  onMounted(() => {
    loadFavorites();
  });

  return {
    favorites: computed(() => favorites.value),
    addFavorite,
    removeFavorite,
    isFavorite,
    toggleFavorite,
    getFavorites,
    loadFavorites
  };
};
