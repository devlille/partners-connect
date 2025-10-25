import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useSponsorsStore } from '~/stores/sponsors';
import type { Partnership } from '~/utils/api';

describe('useSponsorsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  const mockSponsor: Partnership = {
    id: '1',
    company: {
      id: 'company-1',
      name: 'Test Company',
      head_office: {
        address: '123 Test St',
        city: 'Paris',
        zip_code: '75001',
        country: 'France',
      },
      siret: '12345678901234',
      vat: 'FR12345678901',
      site_url: 'https://test.com',
    },
    pack: {
      id: 'pack-1',
      name: 'Gold Pack',
      base_price: 1000,
      nb_tickets: 5,
      with_booth: true,
    },
    status: 'pending',
    contact_name: 'John Doe',
    contact_role: 'CEO',
    language: 'fr',
    phone: '+33123456789',
    emails: ['john@test.com'],
  };

  describe('initial state', () => {
    it('should have empty sponsors array', () => {
      const store = useSponsorsStore();
      expect(store.sponsors).toEqual([]);
    });

    it('should not be loading', () => {
      const store = useSponsorsStore();
      expect(store.loading).toBe(false);
    });

    it('should have no error', () => {
      const store = useSponsorsStore();
      expect(store.error).toBeNull();
    });
  });

  describe('setSponsors', () => {
    it('should set the sponsors list', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);

      expect(store.sponsors).toHaveLength(1);
      expect(store.sponsors[0]).toEqual(mockSponsor);
    });
  });

  describe('addSponsor', () => {
    it('should add a sponsor to the list', () => {
      const store = useSponsorsStore();
      store.addSponsor(mockSponsor);

      expect(store.sponsors).toHaveLength(1);
      expect(store.sponsors[0]).toEqual(mockSponsor);
    });
  });

  describe('updateSponsor', () => {
    it('should update an existing sponsor', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);

      store.updateSponsor('1', { status: 'approved' });

      expect(store.sponsors[0].status).toBe('approved');
    });

    it('should not update if sponsor not found', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);

      store.updateSponsor('999', { status: 'approved' });

      expect(store.sponsors[0].status).toBe('pending');
    });
  });

  describe('removeSponsor', () => {
    it('should remove a sponsor from the list', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);

      store.removeSponsor('1');

      expect(store.sponsors).toHaveLength(0);
    });

    it('should not remove anything if sponsor not found', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);

      store.removeSponsor('999');

      expect(store.sponsors).toHaveLength(1);
      expect(store.sponsors[0]).toEqual(mockSponsor);
    });

    it('should remove only the specified sponsor from multiple sponsors', () => {
      const store = useSponsorsStore();
      const sponsor1 = { ...mockSponsor, id: '1', company: { ...mockSponsor.company, name: 'Company 1' } };
      const sponsor2 = { ...mockSponsor, id: '2', company: { ...mockSponsor.company, name: 'Company 2' } };
      const sponsor3 = { ...mockSponsor, id: '3', company: { ...mockSponsor.company, name: 'Company 3' } };

      store.setSponsors([sponsor1, sponsor2, sponsor3]);

      store.removeSponsor('2');

      expect(store.sponsors).toHaveLength(2);
      expect(store.sponsors.find(s => s.id === '1')).toBeDefined();
      expect(store.sponsors.find(s => s.id === '2')).toBeUndefined();
      expect(store.sponsors.find(s => s.id === '3')).toBeDefined();
    });

    it('should handle removing from empty list', () => {
      const store = useSponsorsStore();

      store.removeSponsor('1');

      expect(store.sponsors).toHaveLength(0);
    });
  });

  describe('getters', () => {
    it('totalSponsors should return the count', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);

      expect(store.totalSponsors).toBe(1);
    });

    it('sponsorsByStatus should filter by status', () => {
      const store = useSponsorsStore();
      const pendingSponsor = { ...mockSponsor, id: '1', status: 'pending' };
      const approvedSponsor = { ...mockSponsor, id: '2', status: 'approved' };

      store.setSponsors([pendingSponsor, approvedSponsor]);

      const pending = store.sponsorsByStatus('pending');
      expect(pending).toHaveLength(1);
      expect(pending[0].id).toBe('1');
    });

    it('sponsorsByPack should filter by pack ID', () => {
      const store = useSponsorsStore();
      const sponsor1 = { ...mockSponsor, id: '1', pack: { ...mockSponsor.pack, id: 'pack-1' } };
      const sponsor2 = { ...mockSponsor, id: '2', pack: { ...mockSponsor.pack, id: 'pack-2' } };

      store.setSponsors([sponsor1, sponsor2]);

      const packSponsors = store.sponsorsByPack('pack-1');
      expect(packSponsors).toHaveLength(1);
      expect(packSponsors[0].id).toBe('1');
    });
  });

  describe('reset', () => {
    it('should reset the store to initial state', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);
      store.setLoading(true);
      store.setError('Test error');

      store.reset();

      expect(store.sponsors).toEqual([]);
      expect(store.loading).toBe(false);
      expect(store.error).toBeNull();
    });
  });
});
