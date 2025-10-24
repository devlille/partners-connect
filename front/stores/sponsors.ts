import { defineStore } from 'pinia';
import type { Partnership } from '~/utils/api';

interface SponsorsState {
  sponsors: Partnership[];
  loading: boolean;
  error: string | null;
}

export const useSponsorsStore = defineStore('sponsors', {
  state: (): SponsorsState => ({
    sponsors: [],
    loading: false,
    error: null,
  }),

  getters: {
    /**
     * Obtenir les sponsors par statut
     */
    sponsorsByStatus: (state) => (status: string) => {
      return state.sponsors.filter(s => s.status === status);
    },

    /**
     * Obtenir le nombre total de sponsors
     */
    totalSponsors: (state) => state.sponsors.length,

    /**
     * Obtenir les sponsors par pack
     */
    sponsorsByPack: (state) => (packId: string) => {
      return state.sponsors.filter(s => s.pack.id === packId);
    },
  },

  actions: {
    /**
     * Définir la liste des sponsors
     */
    setSponsors(sponsors: Partnership[]) {
      this.sponsors = sponsors;
    },

    /**
     * Ajouter un sponsor
     */
    addSponsor(sponsor: Partnership) {
      this.sponsors.push(sponsor);
    },

    /**
     * Mettre à jour un sponsor
     */
    updateSponsor(sponsorId: string, updatedSponsor: Partial<Partnership>) {
      const index = this.sponsors.findIndex(s => s.id === sponsorId);
      if (index !== -1) {
        this.sponsors[index] = { ...this.sponsors[index], ...updatedSponsor };
      }
    },

    /**
     * Supprimer un sponsor
     */
    removeSponsor(sponsorId: string) {
      this.sponsors = this.sponsors.filter(s => s.id !== sponsorId);
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
