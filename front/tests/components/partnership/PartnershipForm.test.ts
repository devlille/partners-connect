import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import PartnershipForm from '~/components/partnership/PartnershipForm.vue';
import type { ExtendedPartnershipItem } from '~/types/partnership';

describe('PartnershipForm', () => {
  const mockPartnership: ExtendedPartnershipItem = {
    id: 'test-id',
    contact: {
      display_name: 'John Doe',
      role: 'CEO'
    },
    company_name: 'Test Company',
    event_name: 'Test Event',
    selected_pack_id: 'pack-1',
    selected_pack_name: 'Gold Pack',
    suggested_pack_id: null,
    suggested_pack_name: null,
    validated_pack_id: null,
    language: 'fr',
    phone: '+33612345678',
    emails: 'test@example.com',
    created_at: '2024-01-01T00:00:00Z',
    validated: false,
    paid: false,
    suggestion: false,
    agreement_generated: false,
    agreement_signed: false,
    option_ids: ['option-1', 'option-2'],
    pack_options: [
      { id: 'option-1', name: 'Option 1', description: 'Description 1' },
      { id: 'option-2', name: 'Option 2', description: null }
    ]
  };

  describe('Props', () => {
    it('should render with partnership data', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership
        }
      });

      expect((wrapper.find('input[placeholder="Nom du contact"]').element as HTMLInputElement).value).toBe('John Doe');
      expect((wrapper.find('input[placeholder="Rôle"]').element as HTMLInputElement).value).toBe('CEO');
      expect((wrapper.find('input[placeholder="email@example.com"]').element as HTMLInputElement).value).toBe('test@example.com');
    });

    it('should show admin actions when showAdminActions is true', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          showAdminActions: true
        }
      });

      // Les boutons d'action doivent être visibles (Annuler, Enregistrer)
      expect(wrapper.find('button[type="button"]').exists()).toBe(true);
      expect(wrapper.find('button[type="submit"]').exists()).toBe(true);
    });

    it('should hide admin actions when showAdminActions is false', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          showAdminActions: false
        }
      });

      // Les boutons d'action ne doivent pas être visibles
      expect(wrapper.find('button[type="button"]').exists()).toBe(false);
      expect(wrapper.find('button[type="submit"]').exists()).toBe(false);
    });

    it('should disable all inputs when readonly is true', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          readonly: true
        }
      });

      const inputs = wrapper.findAll('input');
      inputs.forEach(input => {
        expect(input.attributes('disabled')).toBeDefined();
      });
    });

    it('should enable inputs when readonly is false', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          readonly: false
        }
      });

      const contactNameInput = wrapper.find('input[placeholder="Nom du contact"]');
      expect(contactNameInput.attributes('disabled')).toBeUndefined();
    });

    it('should disable inputs when loading is true', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          loading: true,
          readonly: false
        }
      });

      const inputs = wrapper.findAll('input');
      inputs.forEach(input => {
        expect(input.attributes('disabled')).toBeDefined();
      });
    });
  });

  describe('Emits', () => {
    it('should emit save event with form data on submit', async () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          showAdminActions: true
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      expect(wrapper.emitted()).toHaveProperty('save');
      const saveEvent = wrapper.emitted('save');
      expect(saveEvent).toHaveLength(1);
      expect(saveEvent[0][0]).toMatchObject({
        contact_name: 'John Doe',
        contact_role: 'CEO',
        language: 'fr',
        emails: ['test@example.com'],
        phone: '+33612345678'
      });
    });

    it('should emit cancel event when cancel button is clicked', async () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership,
          showAdminActions: true
        }
      });

      const cancelButton = wrapper.find('button[type="button"]');
      await cancelButton.trigger('click');

      expect(wrapper.emitted()).toHaveProperty('cancel');
      expect(wrapper.emitted('cancel')).toHaveLength(1);
    });

    it('should split multiple emails by comma', async () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: {
            ...mockPartnership,
            emails: 'email1@example.com, email2@example.com'
          },
          showAdminActions: true
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      const saveEvent = wrapper.emitted('save');
      expect(saveEvent[0][0].emails).toEqual(['email1@example.com', 'email2@example.com']);
    });

    it('should handle null phone number', async () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: {
            ...mockPartnership,
            phone: null
          },
          showAdminActions: true
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      const saveEvent = wrapper.emitted('save');
      expect(saveEvent[0][0].phone).toBeNull();
    });
  });

  describe('Display', () => {
    it('should display pack name when selected_pack_name is provided', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership
        }
      });

      expect(wrapper.text()).toContain('Gold Pack');
    });

    it('should display suggested pack when suggested_pack_name is provided', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: {
            ...mockPartnership,
            selected_pack_name: null,
            suggested_pack_name: 'Silver Pack'
          }
        }
      });

      expect(wrapper.text()).toContain('Silver Pack (suggéré)');
    });

    it('should display selected options', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership
        }
      });

      expect(wrapper.text()).toContain('Option 1');
      expect(wrapper.text()).toContain('Description 1');
      expect(wrapper.text()).toContain('Option 2');
    });

    it('should show "Aucune option sélectionnée" when no options', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: {
            ...mockPartnership,
            pack_options: []
          }
        }
      });

      expect(wrapper.text()).toContain('Aucune option sélectionnée');
    });
  });

  describe('Validation', () => {
    it('should require contact_name field', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership
        }
      });

      const contactNameInput = wrapper.find('input[placeholder="Nom du contact"]');
      expect(contactNameInput.attributes('required')).toBeDefined();
    });

    it('should require contact_role field', () => {
      const wrapper = mount(PartnershipForm, {
        props: {
          partnership: mockPartnership
        }
      });

      const contactRoleInput = wrapper.find('input[placeholder="Rôle"]');
      expect(contactRoleInput.attributes('required')).toBeDefined();
    });
  });
});
