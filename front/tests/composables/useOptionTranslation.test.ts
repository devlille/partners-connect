import { describe, it, expect } from 'vitest';
import { useOptionTranslation } from '~/composables/useOptionTranslation';
import type { SponsoringOption } from '~/utils/api';

describe('useOptionTranslation', () => {
  const { getOptionName, getOptionDescription } = useOptionTranslation();

  describe('getOptionName', () => {
    it('should return the name from the first translation', () => {
      const option: SponsoringOption = {
        id: '1',
        translations: {
          fr: { name: 'Option FR', description: 'Description FR' },
          en: { name: 'Option EN', description: 'Description EN' },
        },
        price: 100,
        is_free: false,
      };

      const name = getOptionName(option);
      expect(name).toBe('Option FR');
    });

    it('should return the name from any available translation', () => {
      const option: SponsoringOption = {
        id: '1',
        translations: {
          en: { name: 'Option EN', description: 'Description EN' },
        },
        price: 100,
        is_free: false,
      };

      const name = getOptionName(option);
      expect(name).toBe('Option EN');
    });

    it('should return fallback name when no translations exist', () => {
      const option: SponsoringOption = {
        id: '1',
        name: 'Fallback Name',
        price: 100,
        is_free: false,
      };

      const name = getOptionName(option);
      expect(name).toBe('Fallback Name');
    });

    it('should return default text when no name is available', () => {
      const option: SponsoringOption = {
        id: '1',
        price: 100,
        is_free: false,
      };

      const name = getOptionName(option);
      expect(name).toBe('Option sans nom');
    });
  });

  describe('getOptionDescription', () => {
    it('should return the description from the first translation', () => {
      const option: SponsoringOption = {
        id: '1',
        translations: {
          fr: { name: 'Option FR', description: 'Description FR' },
          en: { name: 'Option EN', description: 'Description EN' },
        },
        price: 100,
        is_free: false,
      };

      const description = getOptionDescription(option);
      expect(description).toBe('Description FR');
    });

    it('should return fallback description when no translations exist', () => {
      const option: SponsoringOption = {
        id: '1',
        name: 'Test',
        description: 'Fallback Description',
        price: 100,
        is_free: false,
      };

      const description = getOptionDescription(option);
      expect(description).toBe('Fallback Description');
    });

    it('should return null when no description is available', () => {
      const option: SponsoringOption = {
        id: '1',
        name: 'Test',
        price: 100,
        is_free: false,
      };

      const description = getOptionDescription(option);
      expect(description).toBeNull();
    });
  });
});
