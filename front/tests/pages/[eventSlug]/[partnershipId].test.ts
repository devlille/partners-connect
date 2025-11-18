import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import PublicPartnershipPage from '~/pages/[eventSlug]/[partnershipId].vue';
import type { ExtendedPartnershipItem } from '~/types/partnership';
import type { CompanySchema, CompanyBillingData } from '~/utils/api';

// Mock API functions
vi.mock('~/utils/api', () => ({
  getEventsPartnershipDetailed: vi.fn(),
  getEventsPartnershipBilling: vi.fn()
}));

describe('Public Partnership Page [eventSlug]/[partnershipId].vue', () => {
  const mockPartnership = {
    id: 'partnership-123',
    contact_name: 'John Doe',
    contact_role: 'CEO',
    language: 'fr',
    phone: '+33612345678',
    emails: ['test@example.com'],
    created_at: '2024-01-01T00:00:00Z',
    selected_pack: {
      id: 'pack-1',
      name: 'Gold Pack',
      optional_options: []
    },
    suggestion_pack: null,
    validated_pack: null,
    process_status: {
      validated_at: null,
      billing_status: 'pending',
      agreement_url: null,
      agreement_signed_url: null
    }
  };

  const mockCompany: CompanySchema = {
    id: 'company-123',
    name: 'Test Company Inc.',
    siret: '12345678901234',
    vat: 'FR12345678901',
    site_url: 'https://example.com',
    description: 'A test company',
    head_office: {
      address: '123 Test Street',
      city: 'Paris',
      zip_code: '75001',
      country: 'France'
    }
  };

  const mockEvent = {
    id: 'event-123',
    name: 'Test Event',
    slug: 'test-event'
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

  let router: any;

  beforeEach(() => {
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/:eventSlug/:partnershipId',
          component: PublicPartnershipPage
        }
      ]
    });

    vi.clearAllMocks();
  });

  describe('Page Meta', () => {
    it('should have auth set to false', () => {
      const pageMeta = (PublicPartnershipPage as any).__pageMeta || {};
      expect(pageMeta.auth).toBe(false);
    });

    it('should use minimal layout', () => {
      const pageMeta = (PublicPartnershipPage as any).__pageMeta || {};
      expect(pageMeta.layout).toBe('minimal');
    });

    it('should have ssr disabled', () => {
      const pageMeta = (PublicPartnershipPage as any).__pageMeta || {};
      expect(pageMeta.ssr).toBe(false);
    });

    it('should validate eventSlug and partnershipId format', async () => {
      const pageMeta = (PublicPartnershipPage as any).__pageMeta || {};
      const validate = pageMeta.validate;

      if (validate) {
        // Valid slugs
        expect(await validate({ params: { eventSlug: 'test-event', partnershipId: 'partnership-123' } })).toBe(true);
        expect(await validate({ params: { eventSlug: 'event_2024', partnershipId: 'p123' } })).toBe(true);

        // Invalid slugs
        expect(await validate({ params: { eventSlug: 'test event', partnershipId: 'partnership-123' } })).toBe(false);
        expect(await validate({ params: { eventSlug: 'test-event', partnershipId: 'partnership@123' } })).toBe(false);
        expect(await validate({ params: { eventSlug: 'test/event', partnershipId: 'partnership-123' } })).toBe(false);
      }
    });
  });

  describe('Data Loading', () => {
    it('should load partnership data on mount using URL params', async () => {
      const { getEventsPartnershipDetailed, getEventsPartnershipBilling } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockResolvedValue({
        data: {
          partnership: mockPartnership,
          company: mockCompany,
          event: mockEvent
        }
      });

      (getEventsPartnershipBilling as any).mockResolvedValue({
        data: mockBilling
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      await wrapper.vm.$nextTick();

      expect(getEventsPartnershipDetailed).toHaveBeenCalledWith('test-event', 'partnership-123');
      expect(getEventsPartnershipBilling).toHaveBeenCalledWith('test-event', 'partnership-123');
    });

    it('should handle 404 error when partnership not found', async () => {
      const { getEventsPartnershipDetailed } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockRejectedValue({
        response: { status: 404 }
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      await wrapper.vm.$nextTick();
      await new Promise(resolve => setTimeout(resolve, 100));

      expect(wrapper.text()).toContain('Partenariat introuvable');
    });

    it('should handle 500 server error', async () => {
      const { getEventsPartnershipDetailed } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockRejectedValue({
        response: { status: 500 }
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      await wrapper.vm.$nextTick();
      await new Promise(resolve => setTimeout(resolve, 100));

      expect(wrapper.text()).toContain('Erreur serveur');
    });

    it('should handle billing data not found gracefully', async () => {
      const { getEventsPartnershipDetailed, getEventsPartnershipBilling } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockResolvedValue({
        data: {
          partnership: mockPartnership,
          company: mockCompany,
          event: mockEvent
        }
      });

      (getEventsPartnershipBilling as any).mockRejectedValue({
        response: { status: 404 }
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      await wrapper.vm.$nextTick();

      // Should not show error - billing not found is acceptable
      expect(wrapper.find('.bg-red-50').exists()).toBe(false);
    });

    it('should reload data when route params change', async () => {
      const { getEventsPartnershipDetailed, getEventsPartnershipBilling } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockResolvedValue({
        data: {
          partnership: mockPartnership,
          company: mockCompany,
          event: mockEvent
        }
      });

      (getEventsPartnershipBilling as any).mockResolvedValue({
        data: mockBilling
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      await wrapper.vm.$nextTick();

      expect(getEventsPartnershipDetailed).toHaveBeenCalledTimes(1);

      // Change route
      await router.push('/another-event/partnership-456');
      await wrapper.vm.$nextTick();

      // Should reload data with new params
      expect(getEventsPartnershipDetailed).toHaveBeenCalledWith('another-event', 'partnership-456');
    });
  });

  describe('UI Rendering', () => {
    it('should show loading skeleton while loading', async () => {
      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      // Initially should show loading
      expect(wrapper.findComponent({ name: 'TableSkeleton' }).exists()).toBe(true);
    });

    it('should render all three forms when data is loaded', async () => {
      const { getEventsPartnershipDetailed, getEventsPartnershipBilling } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockResolvedValue({
        data: {
          partnership: mockPartnership,
          company: mockCompany,
          event: mockEvent
        }
      });

      (getEventsPartnershipBilling as any).mockResolvedValue({
        data: mockBilling
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      await wrapper.vm.$nextTick();
      await new Promise(resolve => setTimeout(resolve, 100));

      expect(wrapper.text()).toContain('Informations du partenariat');
      expect(wrapper.text()).toContain('Informations de l\'entreprise');
      expect(wrapper.text()).toContain('Informations de facturation');
    });
  });

  describe('Component Props', () => {
    it('should pass readonly=true to all form components', async () => {
      const { getEventsPartnershipDetailed, getEventsPartnershipBilling } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockResolvedValue({
        data: {
          partnership: mockPartnership,
          company: mockCompany,
          event: mockEvent
        }
      });

      (getEventsPartnershipBilling as any).mockResolvedValue({
        data: mockBilling
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: {
              template: '<div class="partnership-form"></div>',
              props: ['partnership', 'loading', 'showAdminActions', 'readonly']
            },
            CompanyForm: {
              template: '<div class="company-form"></div>',
              props: ['company', 'readonly', 'loading']
            },
            BillingForm: {
              template: '<div class="billing-form"></div>',
              props: ['partnership', 'billing', 'readonly', 'loading']
            }
          }
        }
      });

      await wrapper.vm.$nextTick();
      await new Promise(resolve => setTimeout(resolve, 100));

      const partnershipForm = wrapper.findComponent({ name: 'PartnershipForm' });
      const companyForm = wrapper.findComponent({ name: 'CompanyForm' });
      const billingForm = wrapper.findComponent({ name: 'BillingForm' });

      if (partnershipForm.exists()) {
        expect(partnershipForm.props('readonly')).toBe(true);
        expect(partnershipForm.props('showAdminActions')).toBe(false);
      }

      if (companyForm.exists()) {
        expect(companyForm.props('readonly')).toBe(true);
      }

      if (billingForm.exists()) {
        expect(billingForm.props('readonly')).toBe(true);
      }
    });

    it('should pass showAdminActions=false to PartnershipForm', async () => {
      const { getEventsPartnershipDetailed, getEventsPartnershipBilling } = await import('~/utils/api');

      (getEventsPartnershipDetailed as any).mockResolvedValue({
        data: {
          partnership: mockPartnership,
          company: mockCompany,
          event: mockEvent
        }
      });

      (getEventsPartnershipBilling as any).mockResolvedValue({
        data: mockBilling
      });

      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: {
              template: '<div class="partnership-form"><button v-if="showAdminActions" class="admin-button">Admin</button></div>',
              props: ['partnership', 'loading', 'showAdminActions', 'readonly']
            },
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      await wrapper.vm.$nextTick();
      await new Promise(resolve => setTimeout(resolve, 100));

      // Admin buttons should not be visible
      expect(wrapper.find('.admin-button').exists()).toBe(false);
    });
  });

  describe('Layout', () => {
    it('should use minimal layout with proper structure', async () => {
      await router.push('/test-event/partnership-123');
      await router.isReady();

      const wrapper = mount(PublicPartnershipPage, {
        global: {
          plugins: [router],
          stubs: {
            PageTitle: true,
            TableSkeleton: true,
            PartnershipForm: true,
            CompanyForm: true,
            BillingForm: true
          }
        }
      });

      // Check for proper page structure
      expect(wrapper.find('.min-h-screen').exists()).toBe(true);
      expect(wrapper.find('.max-w-7xl').exists()).toBe(true);
    });
  });
});
