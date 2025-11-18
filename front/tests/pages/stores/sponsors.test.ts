import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useSponsorsStore } from '~/stores/sponsors';
import type { PartnershipItemSchema } from '~/utils/api';

describe('useSponsorsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  const mockSponsor: PartnershipItemSchema = {
    id: '1',
    contact: {
      display_name: 'John Doe',
      role: 'CEO',
    },
    company_name: 'Test Company',
    event_name: 'DevLille 2025',
    pack_name: 'Gold Pack',
    suggested_pack_name: null,
    language: 'fr',
    phone: '+33123456789',
    emails: 'john@test.com',
    created_at: '2025-01-01T00:00:00Z',
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

      store.updateSponsor('1', { company_name: 'Updated Company' });

      expect(store.sponsors[0]!.company_name).toBe('Updated Company');
    });

    it('should not update if sponsor not found', () => {
      const store = useSponsorsStore();
      store.setSponsors([mockSponsor]);

      store.updateSponsor('999', { company_name: 'Updated Company' });

      expect(store.sponsors[0]!.company_name).toBe('Test Company');
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
      const sponsor1 = { ...mockSponsor, id: '1', company_name: 'Company 1' };
      const sponsor2 = { ...mockSponsor, id: '2', company_name: 'Company 2' };
      const sponsor3 = { ...mockSponsor, id: '3', company_name: 'Company 3' };

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

    it('sponsorsByPack should filter by pack name', () => {
      const store = useSponsorsStore();
      const sponsor1 = { ...mockSponsor, id: '1', pack_name: 'Gold Pack' };
      const sponsor2 = { ...mockSponsor, id: '2', pack_name: 'Silver Pack' };

      store.setSponsors([sponsor1, sponsor2]);

      const packSponsors = store.sponsorsByPack('Gold Pack');
      expect(packSponsors).toHaveLength(1);
      expect(packSponsors[0]!.id).toBe('1');
    });

    it('sponsorsByEvent should filter by event name', () => {
      const store = useSponsorsStore();
      const sponsor1 = { ...mockSponsor, id: '1', event_name: 'DevLille 2025' };
      const sponsor2 = { ...mockSponsor, id: '2', event_name: 'DevLille 2026' };

      store.setSponsors([sponsor1, sponsor2]);

      const eventSponsors = store.sponsorsByEvent('DevLille 2025');
      expect(eventSponsors).toHaveLength(1);
      expect(eventSponsors[0]!.id).toBe('1');
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
