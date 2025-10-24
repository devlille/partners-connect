import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { usePacksStore } from '~/stores/packs';
import type { SponsoringPack } from '~/utils/api';

describe('usePacksStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  const mockPack: SponsoringPack = {
    id: '1',
    name: 'Gold Pack',
    base_price: 1000,
    max_quantity: 10,
    nb_tickets: 5,
    with_booth: true,
    required_options: [],
    optional_options: [],
  };

  describe('initial state', () => {
    it('should have empty packs array', () => {
      const store = usePacksStore();
      expect(store.packs).toEqual([]);
    });

    it('should not be loading', () => {
      const store = usePacksStore();
      expect(store.loading).toBe(false);
    });

    it('should have no error', () => {
      const store = usePacksStore();
      expect(store.error).toBeNull();
    });
  });

  describe('setPacks', () => {
    it('should set the packs list', () => {
      const store = usePacksStore();
      store.setPacks([mockPack]);

      expect(store.packs).toHaveLength(1);
      expect(store.packs[0]).toEqual(mockPack);
    });
  });

  describe('addPack', () => {
    it('should add a pack to the list', () => {
      const store = usePacksStore();
      store.addPack(mockPack);

      expect(store.packs).toHaveLength(1);
      expect(store.packs[0]).toEqual(mockPack);
    });
  });

  describe('updatePack', () => {
    it('should update an existing pack', () => {
      const store = usePacksStore();
      store.setPacks([mockPack]);

      store.updatePack('1', { base_price: 1500 });

      expect(store.packs[0].base_price).toBe(1500);
    });

    it('should not update if pack not found', () => {
      const store = usePacksStore();
      store.setPacks([mockPack]);

      store.updatePack('999', { base_price: 1500 });

      expect(store.packs[0].base_price).toBe(1000);
    });
  });

  describe('removePack', () => {
    it('should remove a pack from the list', () => {
      const store = usePacksStore();
      store.setPacks([mockPack]);

      store.removePack('1');

      expect(store.packs).toHaveLength(0);
    });
  });

  describe('getters', () => {
    it('getPackById should return the correct pack', () => {
      const store = usePacksStore();
      store.setPacks([mockPack]);

      const pack = store.getPackById('1');
      expect(pack).toEqual(mockPack);
    });

    it('getPackById should return undefined if not found', () => {
      const store = usePacksStore();
      store.setPacks([mockPack]);

      const pack = store.getPackById('999');
      expect(pack).toBeUndefined();
    });

    it('packsWithBooth should filter packs with booth', () => {
      const store = usePacksStore();
      const packWithBooth = { ...mockPack, id: '1', with_booth: true };
      const packWithoutBooth = { ...mockPack, id: '2', with_booth: false };

      store.setPacks([packWithBooth, packWithoutBooth]);

      const filtered = store.packsWithBooth;
      expect(filtered).toHaveLength(1);
      expect(filtered[0].id).toBe('1');
    });

    it('packsSortedByPrice should sort packs by price ascending', () => {
      const store = usePacksStore();
      const pack1 = { ...mockPack, id: '1', base_price: 1500 };
      const pack2 = { ...mockPack, id: '2', base_price: 500 };
      const pack3 = { ...mockPack, id: '3', base_price: 1000 };

      store.setPacks([pack1, pack2, pack3]);

      const sorted = store.packsSortedByPrice;
      expect(sorted[0].base_price).toBe(500);
      expect(sorted[1].base_price).toBe(1000);
      expect(sorted[2].base_price).toBe(1500);
    });
  });

  describe('reset', () => {
    it('should reset the store to initial state', () => {
      const store = usePacksStore();
      store.setPacks([mockPack]);
      store.setLoading(true);
      store.setError('Test error');

      store.reset();

      expect(store.packs).toEqual([]);
      expect(store.loading).toBe(false);
      expect(store.error).toBeNull();
    });
  });
});
