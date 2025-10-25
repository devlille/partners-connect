import { defineStore } from 'pinia';
import type { PartnershipItemSchema } from '~/utils/api';
import type { EntityState } from '~/types/generics';

/**
 * Sponsors store state using generic EntityState
 * Reduces code duplication by reusing the EntityState pattern
 */
interface SponsorsState extends EntityState<PartnershipItemSchema> {
  // Items are renamed to 'sponsors' for semantic clarity
  sponsors: PartnershipItemSchema[];
}

export const useSponsorsStore = defineStore('sponsors', {
  state: (): SponsorsState => ({
    sponsors: [],
    items: [], // Required by EntityState<PartnershipItemSchema>
    loading: false,
    error: null,
  }),

  getters: {
    /**
     * Obtenir les sponsors par statut
     */
    sponsorsByStatus: (state) => (status: string) => {
      return state.sponsors.filter((s: PartnershipItemSchema) => s.status === status);
    },

    /**
     * Obtenir le nombre total de sponsors
     */
    totalSponsors: (state) => state.sponsors.length,

    /**
     * Obtenir les sponsors par pack
     */
    sponsorsByPack: (state) => (packId: string) => {
      return state.sponsors.filter((s: PartnershipItemSchema) => s.pack_id === packId);
    },
  },

  actions: {
    /**
     * Définir la liste des sponsors
     */
    setSponsors(sponsors: PartnershipItemSchema[]) {
      this.sponsors = sponsors;
    },

    /**
     * Ajouter un sponsor
     */
    addSponsor(sponsor: PartnershipItemSchema) {
      this.sponsors.push(sponsor);
    },

    /**
     * Mettre à jour un sponsor
     */
    updateSponsor(sponsorId: string, updatedSponsor: Partial<PartnershipItemSchema>) {
      const index = this.sponsors.findIndex((s: PartnershipItemSchema) => s.id === sponsorId);
      if (index !== -1) {
        this.sponsors[index] = { ...this.sponsors[index], ...updatedSponsor };
      }
    },

    /**
     * Supprimer un sponsor
     */
    removeSponsor(sponsorId: string) {
      this.sponsors = this.sponsors.filter((s: PartnershipItemSchema) => s.id !== sponsorId);
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
      this.sponsors = [];
      this.loading = false;
      this.error = null;
    },
  },
});
