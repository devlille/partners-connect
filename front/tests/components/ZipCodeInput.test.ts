import { describe, it, expect, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import ZipCodeInput from '~/components/ZipCodeInput.vue';
import { VALIDATION_MESSAGES } from '~/constants/validation';

describe('ZipCodeInput', () => {
  let wrapper: any;

  beforeEach(() => {
    wrapper = mount(ZipCodeInput, {
      props: {
        modelValue: ''
      }
    });
  });

  describe('Format validation', () => {
    it('should accept valid French zip code (5 digits)', async () => {
      const input = wrapper.find('input');
      await input.setValue('75001');
      await input.trigger('blur');

      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['75001']);
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.ZIP_CODE_INVALID);

      const validationEvents = wrapper.emitted('validation');
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it('should reject zip code with less than 5 digits', async () => {
      const input = wrapper.find('input');
      await input.setValue('750');
      await input.trigger('blur');

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.ZIP_CODE_INVALID);

      const validationEvents = wrapper.emitted('validation');
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it('should limit input to 5 digits', async () => {
      const input = wrapper.find('input');
      await input.setValue('750012345');

      // Le composant limite automatiquement à 5 chiffres
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['75001']);
    });

    it('should reject zip code with non-numeric characters', async () => {
      const input = wrapper.find('input');
      await input.setValue('75A01');

      // Les caractères non-numériques sont automatiquement supprimés
      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['7501']);
    });

    it('should display error message on blur when invalid', async () => {
      const input = wrapper.find('input');
      await input.setValue('750');

      // Pas d'erreur pendant la saisie
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.ZIP_CODE_INVALID);

      await input.trigger('blur');

      // Erreur affichée après le blur
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.ZIP_CODE_INVALID);
    });

    it('should show required error when field is required and empty', async () => {
      wrapper = mount(ZipCodeInput, {
        props: {
          modelValue: '',
          required: true
        }
      });

      const input = wrapper.find('input');
      await input.trigger('blur');

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.REQUIRED);

      const validationEvents = wrapper.emitted('validation');
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });

    it('should accept empty value when not required', async () => {
      const input = wrapper.find('input');
      await input.trigger('blur');

      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.REQUIRED);

      const validationEvents = wrapper.emitted('validation');
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });
  });

  describe('Input sanitization', () => {
    it('should remove all non-numeric characters', async () => {
      const input = wrapper.find('input');
      await input.setValue('75-00 1');

      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['75001']);
    });

    it('should limit input to 5 characters', async () => {
      const input = wrapper.find('input');
      await input.setValue('123456789');

      expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['12345']);
    });
  });

  describe('Common zip codes', () => {
    const validZipCodes = [
      '75001', // Paris
      '69001', // Lyon
      '13001', // Marseille
      '59000', // Lille
      '31000', // Toulouse
      '44000', // Nantes
      '06000', // Nice
      '97400', // Saint-Denis (Réunion)
      '98800', // Nouméa (Nouvelle-Calédonie)
    ];

    validZipCodes.forEach(zipCode => {
      it(`should accept valid zip code: ${zipCode}`, async () => {
        const input = wrapper.find('input');
        await input.setValue(zipCode);
        await input.trigger('blur');

        expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.ZIP_CODE_INVALID);
      });
    });
  });

  describe('Props', () => {
    it('should display custom label', () => {
      wrapper = mount(ZipCodeInput, {
        props: {
          modelValue: '',
          label: 'Code postal personnalisé'
        }
      });

      expect(wrapper.text()).toContain('Code postal personnalisé');
    });

    it('should display custom placeholder', () => {
      wrapper = mount(ZipCodeInput, {
        props: {
          modelValue: '',
          placeholder: '12345'
        }
      });

      const input = wrapper.find('input');
      expect(input.attributes('placeholder')).toBe('12345');
    });

    it('should display hint text when provided', () => {
      wrapper = mount(ZipCodeInput, {
        props: {
          modelValue: '',
          hint: 'Code postal français (5 chiffres)'
        }
      });

      expect(wrapper.text()).toContain('Code postal français (5 chiffres)');
    });

    it('should disable input when disabled prop is true', () => {
      wrapper = mount(ZipCodeInput, {
        props: {
          modelValue: '',
          disabled: true
        }
      });

      const input = wrapper.find('input');
      expect(input.attributes('disabled')).toBeDefined();
    });

    it('should show errors during input when showErrorOnInput is true', async () => {
      wrapper = mount(ZipCodeInput, {
        props: {
          modelValue: '',
          showErrorOnInput: true
        }
      });

      const input = wrapper.find('input');
      await input.setValue('750');

      // Avec showErrorOnInput: true, l'erreur s'affiche pendant la saisie
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.ZIP_CODE_INVALID);
    });

    it('should skip validation when validate is false', async () => {
      wrapper = mount(ZipCodeInput, {
        props: {
          modelValue: '',
          validate: false
        }
      });

      const input = wrapper.find('input');
      await input.setValue('123');
      await input.trigger('blur');

      // Pas d'erreur car validation désactivée
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.ZIP_CODE_INVALID);
    });
  });

  describe('Validation events', () => {
    it('should emit validation event with true for valid zip code', async () => {
      const input = wrapper.find('input');
      await input.setValue('75001');

      const validationEvents = wrapper.emitted('validation');
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it('should emit validation event with false for invalid zip code', async () => {
      const input = wrapper.find('input');
      await input.setValue('750');

      const validationEvents = wrapper.emitted('validation');
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });
  });

  describe('Visual feedback', () => {
    it('should display error message in red', async () => {
      const input = wrapper.find('input');
      await input.setValue('750');
      await input.trigger('blur');

      const errorText = wrapper.find('.text-red-600');
      expect(errorText.exists()).toBe(true);
      expect(errorText.text()).toBe(VALIDATION_MESSAGES.ZIP_CODE_INVALID);
    });
  });

  describe('HTML attributes', () => {
    it('should have inputmode="numeric" for mobile keyboards', () => {
      const input = wrapper.find('input');
      expect(input.attributes('inputmode')).toBe('numeric');
    });

    it('should have maxlength="5"', () => {
      const input = wrapper.find('input');
      expect(input.attributes('maxlength')).toBe('5');
    });

    it('should have type="text"', () => {
      const input = wrapper.find('input');
      expect(input.attributes('type')).toBe('text');
    });
  });
});
