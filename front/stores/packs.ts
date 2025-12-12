import { defineStore } from "pinia";
import type { SponsoringPack } from "~/utils/api";
import type { EntityState } from "~/types/generics";

/**
 * Packs store state using generic EntityState
 * Reduces code duplication by reusing the EntityState pattern
 */
interface PacksState extends EntityState<SponsoringPack> {
  // Items are renamed to 'packs' for semantic clarity
  packs: SponsoringPack[];
}

export const usePacksStore = defineStore("packs", {
  state: (): PacksState => ({
    packs: [],
    items: [], // Required by EntityState<SponsoringPack>
    loading: false,
    error: null,
  }),

  getters: {
    /**
     * Obtenir un pack par son ID
     */
    getPackById: (state) => (packId: string) => {
      return state.packs.find((p) => p.id === packId);
    },

    /**
     * Obtenir les packs avec stand
     */
    packsWithBooth: (state) => {
      return state.packs.filter((p) => p.with_booth);
    },

    /**
     * Obtenir les packs triés par prix
     */
    packsSortedByPrice: (state) => {
      return [...state.packs].sort((a, b) => a.base_price - b.base_price);
    },
  },

  actions: {
    /**
     * Définir la liste des packs
     */
    setPacks(packs: SponsoringPack[]) {
      this.packs = packs;
    },

    /**
     * Ajouter un pack
     */
    addPack(pack: SponsoringPack) {
      this.packs.push(pack);
    },

    /**
     * Mettre à jour un pack
     */
    updatePack(packId: string, updatedPack: Partial<SponsoringPack>) {
      const index = this.packs.findIndex((p) => p.id === packId);
      if (index !== -1) {
        this.packs[index] = { ...this.packs[index], ...updatedPack };
      }
    },

    /**
     * Supprimer un pack
     */
    removePack(packId: string) {
      this.packs = this.packs.filter((p) => p.id !== packId);
    },

    /**
     * Définir l'état de chargement
     */
    setLoading(loading: boolean) {
      this.loading = loading;
    },

    /**
     * Définir l'erreur
     */
    setError(error: string | null) {
      this.error = error;
    },

    /**
     * Réinitialiser le store
     */
    reset() {
      this.packs = [];
      this.loading = false;
      this.error = null;
    },
  },
});
