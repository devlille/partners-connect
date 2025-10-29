import { z } from "zod";

/**
 * Schéma de validation Zod pour un événement favori
 * Limite les tailles de chaînes pour éviter les abus
 */
const FavoriteEventSchema = z.object({
  orgSlug: z.string().min(1).max(100),
  orgName: z.string().min(1).max(200),
  eventSlug: z.string().min(1).max(100),
  eventName: z.string().min(1).max(200),
  addedAt: z.string().datetime(),
});

/**
 * Schéma de validation pour le tableau de favoris
 * Limite à 100 favoris maximum pour éviter de saturer le localStorage
 */
const FavoriteEventsArraySchema = z.array(FavoriteEventSchema).max(100);

export type FavoriteEvent = z.infer<typeof FavoriteEventSchema>;

const STORAGE_KEY = "favorite_events";
const MAX_FAVORITES = 100;

export const useFavoriteEvents = () => {
  const favorites = ref<FavoriteEvent[]>([]);

  /**
   * Charge les favoris depuis le localStorage avec validation Zod
   * Nettoie automatiquement les données corrompues ou invalides
   */
  function loadFavorites() {
    if (import.meta.client) {
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
          const parsed = JSON.parse(stored);

          // Validation des données avec Zod
          const validationResult = FavoriteEventsArraySchema.safeParse(parsed);

          if (validationResult.success) {
            favorites.value = validationResult.data;
          } else {
            console.error(
              "Invalid favorite events data in localStorage:",
              validationResult.error,
            );
            favorites.value = [];
            // Nettoyer les données corrompues
            localStorage.removeItem(STORAGE_KEY);
          }
        }
      } catch (error) {
        console.error("Failed to load favorite events:", error);
        favorites.value = [];
        // Nettoyer les données corrompues
        try {
          localStorage.removeItem(STORAGE_KEY);
        } catch (cleanupError) {
          console.error("Failed to cleanup corrupted data:", cleanupError);
        }
      }
    }
  }

  /**
   * Sauvegarde les favoris dans le localStorage
   * Gère les erreurs de quota dépassé
   */
  function saveFavorites() {
    if (import.meta.client) {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(favorites.value));
      } catch (error) {
        if (
          error instanceof DOMException &&
          error.name === "QuotaExceededError"
        ) {
          console.error(
            "localStorage quota exceeded - unable to save favorites",
          );
          // En production, vous pourriez utiliser un toast pour notifier l'utilisateur
          // useToast().add({ title: 'Limite atteinte', description: 'Impossible d\'ajouter plus de favoris', color: 'error' });
        } else {
          console.error("Failed to save favorite events:", error);
        }
      }
    }
  }

  /**
   * Ajoute un événement aux favoris
   * Vérifie la limite maximale de favoris avant ajout
   * @returns true si l'ajout a réussi, false sinon
   */
  function addFavorite(event: Omit<FavoriteEvent, "addedAt">): boolean {
    const existing = favorites.value.find(
      (f) => f.orgSlug === event.orgSlug && f.eventSlug === event.eventSlug,
    );

    if (existing) {
      return false; // Déjà dans les favoris
    }

    // Vérifier la limite de favoris
    if (favorites.value.length >= MAX_FAVORITES) {
      console.warn(
        `Cannot add favorite: maximum limit of ${MAX_FAVORITES} reached`,
      );
      return false;
    }

    // Valider les données avant ajout
    const newFavorite: FavoriteEvent = {
      ...event,
      addedAt: new Date().toISOString(),
    };

    const validationResult = FavoriteEventSchema.safeParse(newFavorite);

    if (!validationResult.success) {
      console.error("Invalid favorite event data:", validationResult.error);
      return false;
    }

    favorites.value.push(validationResult.data);
    saveFavorites();
    return true;
  }

  /**
   * Retire un événement des favoris
   */
  function removeFavorite(orgSlug: string, eventSlug: string) {
    favorites.value = favorites.value.filter(
      (f) => !(f.orgSlug === orgSlug && f.eventSlug === eventSlug),
    );
    saveFavorites();
  }

  /**
   * Vérifie si un événement est dans les favoris
   */
  function isFavorite(orgSlug: string, eventSlug: string): boolean {
    return favorites.value.some(
      (f) => f.orgSlug === orgSlug && f.eventSlug === eventSlug,
    );
  }

  /**
   * Bascule le statut favori d'un événement
   */
  function toggleFavorite(event: Omit<FavoriteEvent, "addedAt">) {
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
    return [...favorites.value].sort(
      (a, b) => new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime(),
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
    loadFavorites,
  };
};
