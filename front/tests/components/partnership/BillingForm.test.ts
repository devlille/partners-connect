import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BillingForm from '~/components/partnership/BillingForm.vue';
import type { CompanyBillingData } from '~/utils/api';
import type { ExtendedPartnershipItem } from '~/types/partnership';

describe('BillingForm', () => {
  const mockPartnership: ExtendedPartnershipItem = {
    id: 'partnership-123',
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
    option_ids: [],
    pack_options: []
  };

  const mockBilling: CompanyBillingData = {
    name: 'Billing Company Name',
    po: 'PO-12345',
    contact: {
      first_name: 'Jane',
      last_name: 'Smith',
      email: 'jane.smith@example.com'
    }
  };

  describe('Props', () => {
    it('should render with billing data', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling
        }
      });

      expect(wrapper.find('input[placeholder="Nom de facturation"]').element.value).toBe('Billing Company Name');
      expect(wrapper.find('input[placeholder="Numéro de bon de commande"]').element.value).toBe('PO-12345');
      expect(wrapper.find('input[placeholder="Prénom"]').element.value).toBe('Jane');
      expect(wrapper.find('input[placeholder="Nom"]').element.value).toBe('Smith');
      expect(wrapper.find('input[placeholder="email@example.com"]').element.value).toBe('jane.smith@example.com');
    });

    it('should render with empty billing data', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: null
        }
      });

      expect(wrapper.find('input[placeholder="Nom de facturation"]').element.value).toBe('');
      expect(wrapper.find('input[placeholder="Numéro de bon de commande"]').element.value).toBe('');
      expect(wrapper.find('input[placeholder="Prénom"]').element.value).toBe('');
      expect(wrapper.find('input[placeholder="Nom"]').element.value).toBe('');
      expect(wrapper.find('input[placeholder="email@example.com"]').element.value).toBe('');
    });

    it('should disable all inputs when readonly is true', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: true
        }
      });

      const inputs = wrapper.findAll('input');
      inputs.forEach(input => {
        expect(input.attributes('disabled')).toBeDefined();
      });
    });

    it('should enable inputs when readonly is false', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: false
        }
      });

      const firstNameInput = wrapper.find('input[placeholder="Prénom"]');
      expect(firstNameInput.attributes('disabled')).toBeUndefined();
    });

    it('should disable inputs when loading is true', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          loading: true,
          readonly: false
        }
      });

      const inputs = wrapper.findAll('input');
      inputs.forEach(input => {
        expect(input.attributes('disabled')).toBeDefined();
      });
    });

    it('should hide submit button when readonly is true', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: true
        }
      });

      expect(wrapper.find('button[type="submit"]').exists()).toBe(false);
    });

    it('should show submit button when readonly is false', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: false
        }
      });

      expect(wrapper.find('button[type="submit"]').exists()).toBe(true);
    });
  });

  describe('Emits', () => {
    it('should emit save event with form data on submit', async () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: false
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      expect(wrapper.emitted()).toHaveProperty('save');
      const saveEvent = wrapper.emitted('save');
      expect(saveEvent).toHaveLength(1);
      expect(saveEvent[0][0]).toMatchObject({
        name: 'Billing Company Name',
        po: 'PO-12345',
        contact: {
          first_name: 'Jane',
          last_name: 'Smith',
          email: 'jane.smith@example.com'
        }
      });
    });

    it('should trim whitespace from contact fields', async () => {
      const billingWithSpaces: CompanyBillingData = {
        name: '  Company  ',
        po: '  PO-123  ',
        contact: {
          first_name: '  Jane  ',
          last_name: '  Smith  ',
          email: '  jane@example.com  '
        }
      };

      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: billingWithSpaces,
          readonly: false
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      const saveEvent = wrapper.emitted('save');
      expect(saveEvent[0][0]).toMatchObject({
        contact: {
          first_name: 'Jane',
          last_name: 'Smith',
          email: 'jane@example.com'
        }
      });
    });

    it('should handle null optional fields', async () => {
      const billingMinimal: CompanyBillingData = {
        name: null,
        po: null,
        contact: {
          first_name: 'Jane',
          last_name: 'Smith',
          email: 'jane@example.com'
        }
      };

      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: billingMinimal,
          readonly: false
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      const saveEvent = wrapper.emitted('save');
      expect(saveEvent[0][0].name).toBeNull();
      expect(saveEvent[0][0].po).toBeNull();
    });
  });

  describe('Validation', () => {
    it('should require first_name field', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling
        }
      });

      const firstNameInput = wrapper.find('input[placeholder="Prénom"]');
      expect(firstNameInput.attributes('required')).toBeDefined();
    });

    it('should require last_name field', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling
        }
      });

      const lastNameInput = wrapper.find('input[placeholder="Nom"]');
      expect(lastNameInput.attributes('required')).toBeDefined();
    });

    it('should require email field', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling
        }
      });

      const emailInput = wrapper.find('input[placeholder="email@example.com"]');
      expect(emailInput.attributes('required')).toBeDefined();
      expect(emailInput.attributes('type')).toBe('email');
    });

    it('should disable submit button when form is invalid', async () => {
      const invalidBilling: CompanyBillingData = {
        name: null,
        po: null,
        contact: {
          first_name: '',
          last_name: '',
          email: ''
        }
      };

      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: invalidBilling,
          readonly: false
        }
      });

      const submitButton = wrapper.find('button[type="submit"]');
      expect(submitButton.attributes('disabled')).toBeDefined();
    });

    it('should enable submit button when form is valid', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: false
        }
      });

      const submitButton = wrapper.find('button[type="submit"]');
      expect(submitButton.attributes('disabled')).toBeUndefined();
    });
  });

  describe('Form Fields', () => {
    it('should render all billing fields', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling
        }
      });

      expect(wrapper.text()).toContain('Nom de l\'entreprise (optionnel)');
      expect(wrapper.text()).toContain('Bon de commande (optionnel)');
      expect(wrapper.text()).toContain('Contact de facturation');
      expect(wrapper.text()).toContain('Prénom');
      expect(wrapper.text()).toContain('Nom');
      expect(wrapper.text()).toContain('Email');
    });

    it('should update form when billing prop changes', async () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling
        }
      });

      const updatedBilling: CompanyBillingData = {
        ...mockBilling,
        contact: {
          first_name: 'Updated',
          last_name: 'Name',
          email: 'updated@example.com'
        }
      };

      await wrapper.setProps({ billing: updatedBilling });

      expect(wrapper.find('input[placeholder="Prénom"]').element.value).toBe('Updated');
      expect(wrapper.find('input[placeholder="Nom"]').element.value).toBe('Name');
      expect(wrapper.find('input[placeholder="email@example.com"]').element.value).toBe('updated@example.com');
    });
  });

  describe('Loading State', () => {
    it('should show loading state on submit button', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          loading: true,
          readonly: false
        }
      });

      const submitButton = wrapper.find('button[type="submit"]');
      expect(submitButton.attributes('loading')).toBeDefined();
    });

    it('should not show loading state when not loading', () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          loading: false,
          readonly: false
        }
      });

      const submitButton = wrapper.find('button[type="submit"]');
      expect(submitButton.attributes('loading')).toBeUndefined();
    });
  });

  describe('Readonly Behavior', () => {
    it('should not emit save event when readonly', async () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: true
        }
      });

      // Try to submit form (shouldn't be possible as button is hidden)
      await wrapper.find('form').trigger('submit.prevent');

      // No save event should be emitted when readonly
      expect(wrapper.emitted('save')).toBeUndefined();
    });

    it('should allow save event when not readonly', async () => {
      const wrapper = mount(BillingForm, {
        props: {
          partnership: mockPartnership,
          billing: mockBilling,
          readonly: false
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      expect(wrapper.emitted('save')).toHaveLength(1);
    });
  });
});
