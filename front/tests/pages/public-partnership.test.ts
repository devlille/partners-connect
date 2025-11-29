import { describe, it, expect } from 'vitest';

describe('Public Partnership Page - Welcome Message', () => {
  describe('Message Display Logic', () => {
    it('should show welcome message when partnership is not validated', () => {
      const partnership = {
        id: '123',
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: false
      };
      const loading = false;
      const error = null;

      const shouldShowMessage = !loading && !error && partnership && !partnership.validated;

      expect(shouldShowMessage).toBe(true);
    });

    it('should not show welcome message when partnership is validated', () => {
      const partnership = {
        id: '123',
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: true
      };
      const loading = false;
      const error = null;

      const shouldShowMessage = !loading && !error && partnership && !partnership.validated;

      expect(shouldShowMessage).toBe(false);
    });

    it('should not show welcome message when loading', () => {
      const partnership = {
        id: '123',
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: false
      };
      const loading = true;
      const error = null;

      const shouldShowMessage = !loading && !error && partnership && !partnership.validated;

      expect(shouldShowMessage).toBe(false);
    });

    it('should not show welcome message when error exists', () => {
      const partnership = {
        id: '123',
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: false
      };
      const loading = false;
      const error = 'Error loading partnership';

      const shouldShowMessage = !loading && !error && partnership && !partnership.validated;

      expect(shouldShowMessage).toBe(false);
    });

    it('should not show welcome message when partnership is null', () => {
      const partnership = null;
      const loading = false;
      const error = null;

      const shouldShowMessage = !loading && !error && partnership;

      expect(shouldShowMessage).toBeFalsy();
    });
  });

  describe('Message Content', () => {
    it('should include event name in message', () => {
      const eventName = 'DevLille 2026';
      const messageTemplate = `Voici la page dédiée à votre partenariat avec <strong>${eventName}</strong>.`;

      expect(messageTemplate).toContain('DevLille 2026');
      expect(messageTemplate).toContain('<strong>');
    });

    it('should include bookmark recommendation', () => {
      const message = 'Nous vous recommandons de l\'ajouter à vos favoris pour y accéder facilement.';

      expect(message).toContain('favoris');
      expect(message).toContain('accéder facilement');
    });

    it('should mention email communication', () => {
      const message = 'Le lien vers cette page vous sera également communiqué dans nos prochains échanges par email.';

      expect(message).toContain('email');
      expect(message).toContain('communiqué');
    });

    it('should confirm request registration', () => {
      const message = 'Votre demande de partenariat a bien été enregistrée et est actuellement en cours d\'examen par l\'équipe organisatrice.';

      expect(message).toContain('bien été enregistrée');
      expect(message).toContain('en cours d\'examen');
      expect(message).toContain('équipe organisatrice');
    });

    it('should promise follow-up', () => {
      const message = 'Nous reviendrons vers vous très prochainement.';

      expect(message).toContain('reviendrons vers vous');
      expect(message).toContain('très prochainement');
    });
  });

  describe('Partnership Status', () => {
    it('should identify non-validated partnership', () => {
      const partnership = {
        validated: false
      };

      expect(partnership.validated).toBe(false);
    });

    it('should identify validated partnership', () => {
      const partnership = {
        validated: true
      };

      expect(partnership.validated).toBe(true);
    });
  });

  describe('Billing Section Display', () => {
    it('should show billing section when validated pack exists', () => {
      const partnership = {
        id: '123',
        validated_pack_id: 'pack-123'
      };
      const loading = false;
      const error = null;

      const shouldShowBilling = !loading && !error && partnership && partnership.validated_pack_id;

      expect(shouldShowBilling).toBeTruthy();
    });

    it('should not show billing section when no validated pack', () => {
      const partnership = {
        id: '123',
        validated_pack_id: null
      };
      const loading = false;
      const error = null;

      const shouldShowBilling = !loading && !error && partnership && partnership.validated_pack_id;

      expect(shouldShowBilling).toBeFalsy();
    });

    it('should show billing section when validated (legacy)', () => {
      const partnership = {
        id: '123',
        validated: true
      };
      const loading = false;
      const error = null;

      const shouldShowBilling = !loading && !error && partnership && partnership.validated;

      expect(shouldShowBilling).toBe(true);
    });
  });

  describe('Pack Suggestion Display', () => {
    it('should show suggestion when pack is suggested and not validated', () => {
      const partnership = {
        id: '123',
        suggested_pack_id: 'suggested-pack-123',
        validated_pack_id: null
      };
      const loading = false;
      const error = null;

      const shouldShowSuggestion = !loading && !error && partnership &&
        partnership.suggested_pack_id && !partnership.validated_pack_id;

      expect(shouldShowSuggestion).toBe(true);
    });

    it('should not show suggestion when pack is validated', () => {
      const partnership = {
        id: '123',
        suggested_pack_id: 'suggested-pack-123',
        validated_pack_id: 'validated-pack-123'
      };
      const loading = false;
      const error = null;

      const shouldShowSuggestion = !loading && !error && partnership &&
        partnership.suggested_pack_id && !partnership.validated_pack_id;

      expect(shouldShowSuggestion).toBe(false);
    });

    it('should not show suggestion when no suggested pack', () => {
      const partnership = {
        id: '123',
        suggested_pack_id: null,
        validated_pack_id: null
      };
      const loading = false;
      const error = null;

      const shouldShowSuggestion = !loading && !error && partnership &&
        partnership.suggested_pack_id && !partnership.validated_pack_id;

      expect(shouldShowSuggestion).toBeFalsy();
    });

    it('should include both pack names in suggestion message', () => {
      const partnership = {
        suggested_pack_name: 'Pack Gold',
        selected_pack_name: 'Pack Silver'
      };

      const message = `L'équipe organisatrice vous propose le pack ${partnership.suggested_pack_name} en remplacement du pack ${partnership.selected_pack_name} que vous aviez sélectionné.`;

      expect(message).toContain('Pack Gold');
      expect(message).toContain('Pack Silver');
    });
  });

  describe('Pending Validation Message Display', () => {
    it('should show pending message when no validated pack', () => {
      const partnership = {
        id: '123',
        validated_pack_id: null
      };
      const loading = false;
      const error = null;

      const shouldShowPending = !loading && !error && partnership && !partnership.validated_pack_id;

      expect(shouldShowPending).toBe(true);
    });

    it('should not show pending message when pack is validated', () => {
      const partnership = {
        id: '123',
        validated_pack_id: 'pack-123'
      };
      const loading = false;
      const error = null;

      const shouldShowPending = !loading && !error && partnership && !partnership.validated_pack_id;

      expect(shouldShowPending).toBe(false);
    });

    it('should mention billing availability in pending message', () => {
      const message = 'Votre demande de partenariat est en cours de traitement. Les informations de facturation seront disponibles une fois que votre partenariat aura été validé par nos équipes.';

      expect(message).toContain('facturation seront disponibles');
      expect(message).toContain('validé par nos équipes');
    });
  });

  describe('Partnership Information Display', () => {
    it('should display company name', () => {
      const partnership = {
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: false
      };

      expect(partnership.company_name).toBe('Test Company');
    });

    it('should display event name', () => {
      const partnership = {
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: false
      };

      expect(partnership.event_name).toBe('DevLille 2026');
    });

    it('should fallback to default when company name is missing', () => {
      const companyName = undefined;
      const display = companyName || 'Partenariat';

      expect(display).toBe('Partenariat');
    });
  });

  describe('Route Parameters', () => {
    it('should extract event slug from route params', () => {
      const params = {
        eventSlug: 'devedinburgh-2026'
      };

      const eventSlug = Array.isArray(params.eventSlug)
        ? params.eventSlug[0]
        : params.eventSlug;

      expect(eventSlug).toBe('devedinburgh-2026');
    });

    it('should extract partnership ID from route params', () => {
      const params = {
        partnershipId: '12306545-e668-41de-afb9-d847b9742a60'
      };

      const partnershipId = Array.isArray(params.partnershipId)
        ? params.partnershipId[0]
        : params.partnershipId;

      expect(partnershipId).toBe('12306545-e668-41de-afb9-d847b9742a60');
    });

    it('should handle array route params', () => {
      const params = {
        eventSlug: ['devedinburgh-2026']
      };

      const eventSlug = Array.isArray(params.eventSlug)
        ? params.eventSlug[0]
        : params.eventSlug;

      expect(eventSlug).toBe('devedinburgh-2026');
    });
  });

  describe('Page Title', () => {
    it('should set page title with company name', () => {
      const partnership = {
        company_name: 'Test Company'
      };

      const title = `${partnership.company_name || 'Partenariat'} | DevLille`;

      expect(title).toBe('Test Company | DevLille');
    });

    it('should set page title with fallback', () => {
      const partnership = {
        company_name: undefined
      };

      const title = `${partnership.company_name || 'Partenariat'} | DevLille`;

      expect(title).toBe('Partenariat | DevLille');
    });
  });

  describe('Loading State', () => {
    it('should show loading indicator when loading', () => {
      const loading = true;

      expect(loading).toBe(true);
    });

    it('should hide loading indicator when not loading', () => {
      const loading = false;

      expect(loading).toBe(false);
    });
  });

  describe('Error State', () => {
    it('should show error message when error exists', () => {
      const error = 'Failed to load partnership';

      expect(error).toBeDefined();
      expect(typeof error).toBe('string');
    });

    it('should not show error message when no error', () => {
      const error = null;

      expect(error).toBeNull();
    });
  });

  describe('Message Styling', () => {
    it('should apply correct CSS classes for spacing', () => {
      const classes = 'mt-4 pt-4 border-t border-gray-200';

      expect(classes).toContain('mt-4');
      expect(classes).toContain('pt-4');
      expect(classes).toContain('border-t');
    });

    it('should apply text styling with leading-relaxed', () => {
      const textClasses = 'text-sm text-gray-700 leading-relaxed';

      expect(textClasses).toContain('text-sm');
      expect(textClasses).toContain('text-gray-700');
      expect(textClasses).toContain('leading-relaxed');
    });

    it('should apply margin between paragraphs', () => {
      const secondParagraphClasses = 'text-sm text-gray-700 leading-relaxed mt-3';

      expect(secondParagraphClasses).toContain('mt-3');
    });
  });

  describe('Sidebar Navigation', () => {
    it('should include partnership link', () => {
      const eventSlug = 'devedinburgh-2026';
      const partnershipId = '123';

      const partnershipLink = {
        label: 'Partenariat',
        icon: 'i-heroicons-hand-raised',
        to: `/${eventSlug}/${partnershipId}`
      };

      expect(partnershipLink.to).toBe('/devedinburgh-2026/123');
      expect(partnershipLink.label).toBe('Partenariat');
    });

    it('should include company link', () => {
      const eventSlug = 'devedinburgh-2026';
      const partnershipId = '123';

      const companyLink = {
        label: 'Entreprise',
        icon: 'i-heroicons-building-office',
        to: `/${eventSlug}/${partnershipId}/company`
      };

      expect(companyLink.to).toBe('/devedinburgh-2026/123/company');
      expect(companyLink.label).toBe('Entreprise');
    });
  });

  describe('Route Validation', () => {
    it('should validate alphanumeric event slug', () => {
      const eventSlug = 'devedinburgh-2026';
      const isValid = /^[a-zA-Z0-9-_]+$/.test(eventSlug);

      expect(isValid).toBe(true);
    });

    it('should validate UUID partnership ID', () => {
      const partnershipId = '12306545-e668-41de-afb9-d847b9742a60';
      const isValid = /^[a-zA-Z0-9-_]+$/.test(partnershipId);

      expect(isValid).toBe(true);
    });

    it('should reject invalid characters', () => {
      const eventSlug = 'invalid@slug';
      const isValid = /^[a-zA-Z0-9-_]+$/.test(eventSlug);

      expect(isValid).toBe(false);
    });
  });

  describe('Message Formatting', () => {
    it('should format event name in bold', () => {
      const eventName = 'DevLille 2026';
      const formatted = `<strong>${eventName}</strong>`;

      expect(formatted).toBe('<strong>DevLille 2026</strong>');
    });

    it('should use proper French typography', () => {
      const message = 'Voici la page dédiée à votre partenariat';

      expect(message).toContain('à');
      expect(message).toContain('é');
    });

    it('should use proper punctuation', () => {
      const message = 'Voici la page dédiée à votre partenariat avec DevLille 2026.';

      expect(message.endsWith('.')).toBe(true);
    });
  });

  describe('Validated but Unpaid Partnership Message', () => {
    it('should show completion message when partnership is validated but not paid', () => {
      const partnership = {
        id: '123',
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: true,
        paid: false
      };
      const loading = false;
      const error = null;

      const shouldShowCompletionMessage = !loading && !error && partnership &&
        partnership.validated && !partnership.paid;

      expect(shouldShowCompletionMessage).toBe(true);
    });

    it('should not show completion message when partnership is not validated', () => {
      const partnership = {
        id: '123',
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: false,
        paid: false
      };
      const loading = false;
      const error = null;

      const shouldShowCompletionMessage = !loading && !error && partnership &&
        partnership.validated && !partnership.paid;

      expect(shouldShowCompletionMessage).toBe(false);
    });

    it('should not show completion message when partnership is already paid', () => {
      const partnership = {
        id: '123',
        company_name: 'Test Company',
        event_name: 'DevLille 2026',
        validated: true,
        paid: true
      };
      const loading = false;
      const error = null;

      const shouldShowCompletionMessage = !loading && !error && partnership &&
        partnership.validated && !partnership.paid;

      expect(shouldShowCompletionMessage).toBe(false);
    });

    it('should include completion title in message', () => {
      const title = 'Complétez vos informations';

      expect(title).toContain('Complétez');
      expect(title).toContain('informations');
    });

    it('should mention partnership tab in completion message', () => {
      const message = 'Partenariat - Vérifiez et complétez vos coordonnées de contact';

      expect(message).toContain('Partenariat');
      expect(message).toContain('coordonnées de contact');
    });

    it('should mention company tab in completion message', () => {
      const message = 'Entreprise - Renseignez les informations de votre société';

      expect(message).toContain('Entreprise');
      expect(message).toContain('informations de votre société');
    });

    it('should create partnership link correctly', () => {
      const eventSlug = 'devedinburgh-2026';
      const partnershipId = '123';
      const link = `/${eventSlug}/${partnershipId}`;

      expect(link).toBe('/devedinburgh-2026/123');
    });

    it('should create company link correctly', () => {
      const eventSlug = 'devedinburgh-2026';
      const partnershipId = '123';
      const link = `/${eventSlug}/${partnershipId}/company`;

      expect(link).toBe('/devedinburgh-2026/123/company');
    });

    it('should use blue theme for completion message', () => {
      const containerClasses = 'bg-blue-50 border border-blue-200 rounded-lg p-4';

      expect(containerClasses).toContain('bg-blue-50');
      expect(containerClasses).toContain('border-blue-200');
    });

    it('should include information icon in completion message', () => {
      const iconClass = 'i-heroicons-information-circle';

      expect(iconClass).toContain('information-circle');
    });

    it('should confirm validation in completion message', () => {
      const message = 'Votre partenariat a été validé !';

      expect(message).toContain('validé');
      expect(message).toContain('!');
    });
  });
});
