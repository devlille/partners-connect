import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import CompanyForm from '~/components/partnership/CompanyForm.vue';
import type { CompanySchema } from '~/utils/api';

describe('CompanyForm', () => {
  const mockCompany: CompanySchema = {
    id: 'company-123',
    name: 'Test Company Inc.',
    siret: '12345678901234',
    vat: 'FR12345678901',
    site_url: 'https://example.com',
    description: 'A test company description',
    head_office: {
      address: '123 Test Street',
      city: 'Paris',
      zip_code: '75001',
      country: 'France'
    }
  };

  describe('Props', () => {
    it('should render with company data', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany
        }
      });

      expect(wrapper.find('input[placeholder="Nom de l\'entreprise"]').element.value).toBe('Test Company Inc.');
      expect(wrapper.find('input[placeholder="00000000000000"]').element.value).toBe('12345678901234');
      expect(wrapper.find('input[placeholder="FR00000000000"]').element.value).toBe('FR12345678901');
      expect(wrapper.find('input[placeholder="https://example.com"]').element.value).toBe('https://example.com');
    });

    it('should render address fields with company data', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany
        }
      });

      expect(wrapper.find('input[placeholder="Rue, numéro..."]').element.value).toBe('123 Test Street');
      expect(wrapper.find('input[placeholder="00000"]').element.value).toBe('75001');
      expect(wrapper.find('input[placeholder="Ville"]').element.value).toBe('Paris');
      expect(wrapper.find('input[placeholder="France"]').element.value).toBe('France');
    });

    it('should disable all inputs when readonly is true', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: true
        }
      });

      const inputs = wrapper.findAll('input');
      inputs.forEach(input => {
        expect(input.attributes('disabled')).toBeDefined();
      });

      const textarea = wrapper.find('textarea');
      expect(textarea.attributes('disabled')).toBeDefined();
    });

    it('should enable inputs when readonly is false', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      const nameInput = wrapper.find('input[placeholder="Nom de l\'entreprise"]');
      expect(nameInput.attributes('disabled')).toBeUndefined();
    });

    it('should disable inputs when loading is true', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
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
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: true
        }
      });

      expect(wrapper.find('button[type="submit"]').exists()).toBe(false);
    });

    it('should show submit button when readonly is false', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      expect(wrapper.find('button[type="submit"]').exists()).toBe(true);
    });
  });

  describe('Emits', () => {
    it('should emit save event with trimmed form data on submit', async () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      expect(wrapper.emitted()).toHaveProperty('save');
      const saveEvent = wrapper.emitted('save');
      expect(saveEvent).toHaveLength(1);
      expect(saveEvent[0][0]).toMatchObject({
        name: 'Test Company Inc.',
        siret: '12345678901234',
        vat: 'FR12345678901',
        site_url: 'https://example.com',
        description: 'A test company description',
        head_office: {
          address: '123 Test Street',
          city: 'Paris',
          zip_code: '75001',
          country: 'France'
        }
      });
    });

    it('should trim whitespace from all fields before emitting', async () => {
      const companyWithSpaces = {
        ...mockCompany,
        name: '  Test Company  ',
        siret: '  12345678901234  ',
        description: '  Description  ',
        head_office: {
          address: '  123 Test Street  ',
          city: '  Paris  ',
          zip_code: '  75001  ',
          country: '  France  '
        }
      };

      const wrapper = mount(CompanyForm, {
        props: {
          company: companyWithSpaces,
          readonly: false
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      const saveEvent = wrapper.emitted('save');
      expect(saveEvent[0][0]).toMatchObject({
        name: 'Test Company',
        siret: '12345678901234',
        description: 'Description',
        head_office: {
          address: '123 Test Street',
          city: 'Paris',
          zip_code: '75001',
          country: 'France'
        }
      });
    });

    it('should handle null values correctly', async () => {
      const companyWithNulls: CompanySchema = {
        id: 'company-123',
        name: 'Test Company',
        siret: null,
        vat: null,
        site_url: null,
        description: null,
        head_office: {
          address: '',
          city: '',
          zip_code: '',
          country: ''
        }
      };

      const wrapper = mount(CompanyForm, {
        props: {
          company: companyWithNulls,
          readonly: false
        }
      });

      await wrapper.find('form').trigger('submit.prevent');

      const saveEvent = wrapper.emitted('save');
      expect(saveEvent[0][0].siret).toBeNull();
      expect(saveEvent[0][0].vat).toBeNull();
      expect(saveEvent[0][0].site_url).toBeNull();
      expect(saveEvent[0][0].description).toBeNull();
    });
  });

  describe('Form Fields', () => {
    it('should render all required company fields', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany
        }
      });

      expect(wrapper.find('label:contains("Nom de l\'entreprise")').exists()).toBe(true);
      expect(wrapper.find('label:contains("SIRET")').exists()).toBe(true);
      expect(wrapper.find('label:contains("TVA")').exists()).toBe(true);
      expect(wrapper.find('label:contains("Site web")').exists()).toBe(true);
      expect(wrapper.find('label:contains("Description")').exists()).toBe(true);
    });

    it('should render all address fields', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany
        }
      });

      expect(wrapper.text()).toContain('Adresse du siège social');
      expect(wrapper.find('label:contains("Adresse")').exists()).toBe(true);
      expect(wrapper.find('label:contains("Code postal")').exists()).toBe(true);
      expect(wrapper.find('label:contains("Ville")').exists()).toBe(true);
      expect(wrapper.find('label:contains("Pays")').exists()).toBe(true);
    });

    it('should update form when company prop changes', async () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany
        }
      });

      const updatedCompany = {
        ...mockCompany,
        name: 'Updated Company Name'
      };

      await wrapper.setProps({ company: updatedCompany });

      expect(wrapper.find('input[placeholder="Nom de l\'entreprise"]').element.value).toBe('Updated Company Name');
    });
  });

  describe('Loading State', () => {
    it('should show loading state on submit button', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          loading: true,
          readonly: false
        }
      });

      const submitButton = wrapper.find('button[type="submit"]');
      expect(submitButton.attributes('loading')).toBeDefined();
    });

    it('should not show loading state when not loading', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          loading: false,
          readonly: false
        }
      });

      const submitButton = wrapper.find('button[type="submit"]');
      expect(submitButton.attributes('loading')).toBeUndefined();
    });
  });

  describe('Social Networks', () => {
    it('should render social networks section', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany
        }
      });

      expect(wrapper.text()).toContain('Réseaux sociaux');
    });

    it('should show "Aucun réseau social défini" when no socials', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany
        }
      });

      expect(wrapper.text()).toContain('Aucun réseau social défini');
    });

    it('should show add button when not readonly', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      const buttons = wrapper.findAll('button');
      const addButton = buttons.find(btn => btn.text().includes('+ Ajouter un réseau'));
      expect(addButton).toBeDefined();
    });

    it('should hide add button when readonly', () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: true
        }
      });

      const buttons = wrapper.findAll('button');
      const addButton = buttons.find(btn => btn.text().includes('+ Ajouter un réseau'));
      expect(addButton).toBeUndefined();
    });

    it('should add a social network when add button is clicked', async () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      const buttons = wrapper.findAll('button');
      const addButton = buttons.find(btn => btn.text().includes('+ Ajouter un réseau'));
      expect(addButton).toBeDefined();

      if (addButton) {
        await addButton.trigger('click');

        // Should no longer show "Aucun réseau social défini"
        expect(wrapper.text()).not.toContain('Aucun réseau social défini');

        // Should show an input for the URL
        const urlInputs = wrapper.findAll('input[placeholder="https://..."]');
        expect(urlInputs.length).toBe(1);
      }
    });

    it('should include socials in save event when valid', async () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      // Add a social network
      const buttons = wrapper.findAll('button');
      const addButton = buttons.find(btn => btn.text().includes('+ Ajouter un réseau'));

      if (addButton) {
        await addButton.trigger('click');

        // Find the URL input and set a value
        const urlInput = wrapper.find('input[placeholder="https://..."]');
        await urlInput.setValue('https://linkedin.com/company/test');

        await wrapper.find('form').trigger('submit.prevent');

        const saveEvent = wrapper.emitted('save');
        expect(saveEvent).toBeDefined();
        if (saveEvent) {
          expect(saveEvent[0][0].socials).toBeDefined();
          expect(saveEvent[0][0].socials).toHaveLength(1);
          expect(saveEvent[0][0].socials[0]).toMatchObject({
            type: 'LINKEDIN',
            url: 'https://linkedin.com/company/test'
          });
        }
      }
    });

    it('should not include socials in save event when empty URLs', async () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      // Add a social network but don't set URL
      const buttons = wrapper.findAll('button');
      const addButton = buttons.find(btn => btn.text().includes('+ Ajouter un réseau'));

      if (addButton) {
        await addButton.trigger('click');

        await wrapper.find('form').trigger('submit.prevent');

        const saveEvent = wrapper.emitted('save');
        expect(saveEvent).toBeDefined();
        if (saveEvent) {
          expect(saveEvent[0][0].socials).toBeNull();
        }
      }
    });

    it('should remove a social network when remove button is clicked', async () => {
      const wrapper = mount(CompanyForm, {
        props: {
          company: mockCompany,
          readonly: false
        }
      });

      // Add two social networks
      const buttons = wrapper.findAll('button');
      const addButton = buttons.find(btn => btn.text().includes('+ Ajouter un réseau'));

      if (addButton) {
        await addButton.trigger('click');
        await addButton.trigger('click');

        // Find all remove buttons (-)
        const allButtons = wrapper.findAll('button');
        const removeButtons = allButtons.filter(btn => btn.text() === '-');
        expect(removeButtons.length).toBe(2);

        await removeButtons[0].trigger('click');

        // Should have only one remove button left
        const allButtonsAfter = wrapper.findAll('button');
        const remainingRemoveButtons = allButtonsAfter.filter(btn => btn.text() === '-');
        expect(remainingRemoveButtons.length).toBe(1);
      }
    });

    it('should initialize socials from company prop if available', async () => {
      const companyWithSocials = {
        ...mockCompany,
        socials: [
          { type: 'LINKEDIN', url: 'https://linkedin.com/company/test' },
          { type: 'X', url: 'https://x.com/test' }
        ]
      } as any;

      const wrapper = mount(CompanyForm, {
        props: {
          company: companyWithSocials
        }
      });

      // Wait for onMounted to execute
      await wrapper.vm.$nextTick();

      // Should show the social networks
      expect(wrapper.text()).not.toContain('Aucun réseau social défini');

      const urlInputs = wrapper.findAll('input[placeholder="https://..."]');
      expect(urlInputs.length).toBe(2);
      expect((urlInputs[0].element as HTMLInputElement).value).toBe('https://linkedin.com/company/test');
      expect((urlInputs[1].element as HTMLInputElement).value).toBe('https://x.com/test');
    });
  });
});
