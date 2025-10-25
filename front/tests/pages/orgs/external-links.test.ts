import { describe, it, expect } from 'vitest';

describe('External Links Page - Helper Functions', () => {
  describe('URL Validation', () => {
    it('should validate URL with https protocol', () => {
      const url = 'https://example.com';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should validate URL with http protocol', () => {
      const url = 'http://example.com';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should reject invalid URL', () => {
      const url = 'not-a-valid-url';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(false);
    });

    it('should reject empty URL', () => {
      const url = '';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(false);
    });

    it('should validate URL with path', () => {
      const url = 'https://example.com/path/to/page';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should validate URL with query parameters', () => {
      const url = 'https://example.com/path?param=value';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should validate URL with fragment', () => {
      const url = 'https://example.com/path#section';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should reject URL without protocol', () => {
      const url = 'example.com';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(false);
    });

    it('should validate URL with subdomain', () => {
      const url = 'https://subdomain.example.com';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should validate URL with port', () => {
      const url = 'https://example.com:8080';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });
  });

  describe('Name Validation', () => {
    it('should validate required name', () => {
      const name = '';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBeFalsy();
    });

    it('should validate non-empty name', () => {
      const name = 'Site web';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should trim whitespace from name', () => {
      const name = '  Site web  ';
      const trimmed = name.trim();
      expect(trimmed).toBe('Site web');
    });

    it('should reject whitespace-only name', () => {
      const name = '   ';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBeFalsy();
    });

    it('should accept name with special characters', () => {
      const name = 'Site web - Billetterie & Programme';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should accept name with numbers', () => {
      const name = 'Billetterie 2025';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should accept name with accents', () => {
      const name = 'Événement spécial';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });
  });

  describe('Form Validation', () => {
    it('should validate complete form data', () => {
      const formData = {
        name: 'Site web',
        url: 'https://example.com'
      };

      const nameValid = formData.name && formData.name.trim().length > 0;
      const urlValid = formData.url && formData.url.trim().length > 0;

      let urlFormatValid = false;
      try {
        new URL(formData.url);
        urlFormatValid = true;
      } catch {
        urlFormatValid = false;
      }

      expect(nameValid).toBe(true);
      expect(urlValid).toBe(true);
      expect(urlFormatValid).toBe(true);
    });

    it('should detect missing name', () => {
      const formData = {
        name: '',
        url: 'https://example.com'
      };

      const nameValid = formData.name && formData.name.trim().length > 0;
      expect(nameValid).toBeFalsy();
    });

    it('should detect missing URL', () => {
      const formData = {
        name: 'Site web',
        url: ''
      };

      const urlValid = formData.url && formData.url.trim().length > 0;
      expect(urlValid).toBeFalsy();
    });

    it('should detect invalid URL format', () => {
      const formData = {
        name: 'Site web',
        url: 'invalid-url'
      };

      let urlFormatValid = false;
      try {
        new URL(formData.url);
        urlFormatValid = true;
      } catch {
        urlFormatValid = false;
      }

      expect(urlFormatValid).toBe(false);
    });

    it('should validate form with trimmed values', () => {
      const formData = {
        name: '  Site web  ',
        url: '  https://example.com  '
      };

      const trimmedName = formData.name.trim();
      const trimmedUrl = formData.url.trim();

      const nameValid = trimmedName.length > 0;

      let urlValid = false;
      try {
        new URL(trimmedUrl);
        urlValid = true;
      } catch {
        urlValid = false;
      }

      expect(nameValid).toBe(true);
      expect(urlValid).toBe(true);
    });
  });

  describe('Link Data Construction', () => {
    it('should construct external link object', () => {
      const formData = {
        name: 'Site web',
        url: 'https://example.com'
      };

      const linkData = {
        name: formData.name,
        url: formData.url
      };

      expect(linkData.name).toBe('Site web');
      expect(linkData.url).toBe('https://example.com');
    });

    it('should construct link with complex URL', () => {
      const formData = {
        name: 'Billetterie',
        url: 'https://billetterie.example.com/event/2025?promo=early'
      };

      const linkData = {
        name: formData.name,
        url: formData.url
      };

      expect(linkData.name).toBe('Billetterie');
      expect(linkData.url).toBe('https://billetterie.example.com/event/2025?promo=early');
    });

    it('should construct link with accented name', () => {
      const formData = {
        name: 'Événement spécial',
        url: 'https://example.com'
      };

      const linkData = {
        name: formData.name,
        url: formData.url
      };

      expect(linkData.name).toBe('Événement spécial');
    });
  });

  describe('Form Reset', () => {
    it('should reset form to default values', () => {
      const defaultForm = {
        name: '',
        url: ''
      };

      expect(defaultForm.name).toBe('');
      expect(defaultForm.url).toBe('');
    });

    it('should clear form after submission', () => {
      const form = {
        name: 'Site web',
        url: 'https://example.com'
      };

      // Reset
      form.name = '';
      form.url = '';

      expect(form.name).toBe('');
      expect(form.url).toBe('');
    });
  });

  describe('Error Messages', () => {
    it('should generate error for missing name', () => {
      const name = '';
      let error = null;

      if (!name || !name.trim()) {
        error = 'Le nom est obligatoire';
      }

      expect(error).toBe('Le nom est obligatoire');
    });

    it('should generate error for missing URL', () => {
      const url = '';
      let error = null;

      if (!url || !url.trim()) {
        error = 'L\'URL est obligatoire';
      }

      expect(error).toBe('L\'URL est obligatoire');
    });

    it('should generate error for invalid URL', () => {
      const url = 'invalid-url';
      let error = null;

      try {
        new URL(url);
      } catch {
        error = 'L\'URL n\'est pas valide';
      }

      expect(error).toBe('L\'URL n\'est pas valide');
    });

    it('should not generate error for valid data', () => {
      const name = 'Site web';
      const url = 'https://example.com';
      let error = null;

      if (!name || !name.trim()) {
        error = 'Le nom est obligatoire';
      } else if (!url || !url.trim()) {
        error = 'L\'URL est obligatoire';
      } else {
        try {
          new URL(url);
        } catch {
          error = 'L\'URL n\'est pas valide';
        }
      }

      expect(error).toBeNull();
    });
  });

  describe('Name Length Validation', () => {
    it('should accept name at reasonable length', () => {
      const name = 'A'.repeat(100);
      const isValid = name.length > 0 && name.length <= 200;
      expect(isValid).toBe(true);
    });

    it('should accept name at max length', () => {
      const name = 'A'.repeat(200);
      const isValid = name.length > 0 && name.length <= 200;
      expect(isValid).toBe(true);
    });

    it('should reject name exceeding max length', () => {
      const name = 'A'.repeat(201);
      const isValid = name.length > 0 && name.length <= 200;
      expect(isValid).toBe(false);
    });

    it('should accept short name', () => {
      const name = 'Web';
      const isValid = name.length > 0 && name.length <= 200;
      expect(isValid).toBe(true);
    });

    it('should accept single character name', () => {
      const name = 'A';
      const isValid = name.length > 0 && name.length <= 200;
      expect(isValid).toBe(true);
    });
  });

  describe('URL Length Validation', () => {
    it('should accept URL at reasonable length', () => {
      const url = 'https://example.com/' + 'a'.repeat(400);
      const isValid = url.length <= 500;
      expect(isValid).toBe(true);
    });

    it('should accept URL at max length', () => {
      const url = 'https://example.com/' + 'a'.repeat(480);
      const isValid = url.length <= 500;
      expect(isValid).toBe(true);
    });

    it('should reject URL exceeding max length', () => {
      const url = 'https://example.com/' + 'a'.repeat(481);
      const isValid = url.length <= 500;
      expect(isValid).toBe(false);
    });

    it('should accept short URL', () => {
      const url = 'https://ex.co';
      const isValid = url.length <= 500;
      expect(isValid).toBe(true);
    });
  });

  describe('URL Protocol Handling', () => {
    it('should preserve https protocol', () => {
      const url = 'https://example.com';
      const urlObj = new URL(url);
      expect(urlObj.protocol).toBe('https:');
    });

    it('should preserve http protocol', () => {
      const url = 'http://example.com';
      const urlObj = new URL(url);
      expect(urlObj.protocol).toBe('http:');
    });

    it('should extract hostname from URL', () => {
      const url = 'https://example.com/path';
      const urlObj = new URL(url);
      expect(urlObj.hostname).toBe('example.com');
    });

    it('should extract path from URL', () => {
      const url = 'https://example.com/path/to/page';
      const urlObj = new URL(url);
      expect(urlObj.pathname).toBe('/path/to/page');
    });
  });

  describe('Link Display', () => {
    it('should format link for display', () => {
      const link = {
        id: '1',
        name: 'Site web',
        url: 'https://example.com'
      };

      expect(link.name).toBe('Site web');
      expect(link.url).toBe('https://example.com');
      expect(link.id).toBe('1');
    });

    it('should handle link with long URL', () => {
      const link = {
        id: '2',
        name: 'Billetterie',
        url: 'https://very-long-domain-name.example.com/with/a/very/long/path/to/the/page'
      };

      const displayUrl = link.url.length > 50
        ? link.url.substring(0, 50) + '...'
        : link.url;

      expect(displayUrl).toContain('...');
      expect(displayUrl.length).toBeLessThanOrEqual(53);
    });

    it('should not truncate short URL', () => {
      const link = {
        id: '3',
        name: 'Site',
        url: 'https://example.com'
      };

      const displayUrl = link.url.length > 50
        ? link.url.substring(0, 50) + '...'
        : link.url;

      expect(displayUrl).toBe('https://example.com');
      expect(displayUrl).not.toContain('...');
    });
  });

  describe('Link Collection Handling', () => {
    it('should handle empty links array', () => {
      const links: any[] = [];
      expect(links.length).toBe(0);
    });

    it('should handle single link', () => {
      const links = [
        { id: '1', name: 'Site web', url: 'https://example.com' }
      ];
      expect(links.length).toBe(1);
      expect(links[0].name).toBe('Site web');
    });

    it('should handle multiple links', () => {
      const links = [
        { id: '1', name: 'Site web', url: 'https://example.com' },
        { id: '2', name: 'Billetterie', url: 'https://tickets.example.com' },
        { id: '3', name: 'Programme', url: 'https://programme.example.com' }
      ];
      expect(links.length).toBe(3);
    });

    it('should find link by id', () => {
      const links = [
        { id: '1', name: 'Site web', url: 'https://example.com' },
        { id: '2', name: 'Billetterie', url: 'https://tickets.example.com' }
      ];

      const found = links.find(link => link.id === '2');
      expect(found).toBeDefined();
      expect(found?.name).toBe('Billetterie');
    });

    it('should remove link by id', () => {
      const links = [
        { id: '1', name: 'Site web', url: 'https://example.com' },
        { id: '2', name: 'Billetterie', url: 'https://tickets.example.com' }
      ];

      const filtered = links.filter(link => link.id !== '1');
      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('2');
    });
  });

  describe('Special Characters in Name', () => {
    it('should accept name with hyphens', () => {
      const name = 'Site-web-officiel';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should accept name with parentheses', () => {
      const name = 'Site web (officiel)';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should accept name with apostrophes', () => {
      const name = 'L\'événement';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should accept name with ampersand', () => {
      const name = 'Billetterie & Programme';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should accept name with slash', () => {
      const name = 'Infos/Contact';
      const isValid = name && name.trim().length > 0;
      expect(isValid).toBe(true);
    });
  });

  describe('URL Edge Cases', () => {
    it('should validate URL with IP address', () => {
      const url = 'https://192.168.1.1';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should validate URL with localhost', () => {
      const url = 'http://localhost:3000';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should validate URL with multiple subdomains', () => {
      const url = 'https://sub1.sub2.example.com';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });

    it('should validate URL with authentication', () => {
      const url = 'https://user:pass@example.com';
      let isValid = false;
      try {
        new URL(url);
        isValid = true;
      } catch {
        isValid = false;
      }
      expect(isValid).toBe(true);
    });
  });
});
