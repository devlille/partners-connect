import { describe, it, expect } from 'vitest';

describe('Job Offers Page - Helper Functions', () => {
  describe('Date Formatting', () => {
    function formatDate(dateString: string): string {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }

    it('should format ISO date string to French format', () => {
      const result = formatDate('2025-01-15');
      expect(result).toContain('2025');
      expect(result).toContain('janvier');
      expect(result).toContain('15');
    });

    it('should handle different months', () => {
      const result = formatDate('2025-06-20');
      expect(result).toContain('2025');
      expect(result).toContain('juin');
      expect(result).toContain('20');
    });

    it('should handle December dates', () => {
      const result = formatDate('2024-12-31');
      expect(result).toContain('2024');
      expect(result).toContain('décembre');
      expect(result).toContain('31');
    });
  });

  describe('Job Form Validation', () => {
    it('should validate required title', () => {
      const title = '';
      const isValid = title && title.trim().length > 0;
      expect(isValid).toBeFalsy();
    });

    it('should validate non-empty title', () => {
      const title = 'Développeur Full Stack';
      const isValid = title && title.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should trim whitespace from title', () => {
      const title = '  Développeur Full Stack  ';
      const trimmed = title.trim();
      expect(trimmed).toBe('Développeur Full Stack');
    });

    it('should validate required location', () => {
      const location = '';
      const isValid = location && location.trim().length > 0;
      expect(isValid).toBeFalsy();
    });

    it('should validate non-empty location', () => {
      const location = 'Paris, France';
      const isValid = location && location.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should validate required URL', () => {
      const url = '';
      const isValid = url && url.trim().length > 0;
      expect(isValid).toBeFalsy();
    });

    it('should validate non-empty URL', () => {
      const url = 'https://example.com/jobs/123';
      const isValid = url && url.trim().length > 0;
      expect(isValid).toBe(true);
    });

    it('should validate required publication date', () => {
      const publicationDate = '';
      const isValid = !!publicationDate;
      expect(isValid).toBe(false);
    });

    it('should validate non-empty publication date', () => {
      const publicationDate = '2025-01-15';
      const isValid = !!publicationDate;
      expect(isValid).toBe(true);
    });
  });

  describe('Optional Fields Handling', () => {
    it('should handle null end_date', () => {
      const endDate: string | null = null;
      const value = endDate || null;
      expect(value).toBeNull();
    });

    it('should handle empty string end_date as null', () => {
      const endDate = '';
      const value = endDate || null;
      expect(value).toBeNull();
    });

    it('should preserve valid end_date', () => {
      const endDate = '2025-12-31';
      const value = endDate || null;
      expect(value).toBe('2025-12-31');
    });

    it('should handle null experience_years', () => {
      const experienceYears: number | null = null;
      const value = experienceYears || null;
      expect(value).toBeNull();
    });

    it('should handle zero experience_years as null', () => {
      const experienceYears = 0;
      const value = experienceYears || null;
      expect(value).toBeNull();
    });

    it('should preserve valid experience_years', () => {
      const experienceYears = 5;
      const value = experienceYears || null;
      expect(value).toBe(5);
    });

    it('should handle null salary', () => {
      const salary: string | null = null;
      const value = salary || null;
      expect(value).toBeNull();
    });

    it('should handle empty string salary as null', () => {
      const salary = '';
      const value = salary || null;
      expect(value).toBeNull();
    });

    it('should preserve valid salary', () => {
      const salary = '40k-50k €';
      const value = salary || null;
      expect(value).toBe('40k-50k €');
    });
  });

  describe('Experience Years Validation', () => {
    it('should validate experience years minimum', () => {
      const experienceYears = 1;
      const isValid = experienceYears >= 1 && experienceYears <= 20;
      expect(isValid).toBe(true);
    });

    it('should validate experience years maximum', () => {
      const experienceYears = 20;
      const isValid = experienceYears >= 1 && experienceYears <= 20;
      expect(isValid).toBe(true);
    });

    it('should reject experience years below minimum', () => {
      const experienceYears = 0;
      const isValid = experienceYears >= 1 && experienceYears <= 20;
      expect(isValid).toBe(false);
    });

    it('should reject experience years above maximum', () => {
      const experienceYears = 21;
      const isValid = experienceYears >= 1 && experienceYears <= 20;
      expect(isValid).toBe(false);
    });

    it('should accept mid-range experience years', () => {
      const experienceYears = 10;
      const isValid = experienceYears >= 1 && experienceYears <= 20;
      expect(isValid).toBe(true);
    });
  });

  describe('URL Validation', () => {
    it('should validate URL format', () => {
      const urlRegex = /^https?:\/\/.+/;
      expect(urlRegex.test('https://example.com')).toBe(true);
      expect(urlRegex.test('http://example.com')).toBe(true);
    });

    it('should reject invalid URL format', () => {
      const urlRegex = /^https?:\/\/.+/;
      expect(urlRegex.test('not-a-url')).toBe(false);
      expect(urlRegex.test('ftp://example.com')).toBe(false);
    });

    it('should validate URL with path', () => {
      const urlRegex = /^https?:\/\/.+/;
      expect(urlRegex.test('https://example.com/jobs/123')).toBe(true);
    });

    it('should validate URL with query params', () => {
      const urlRegex = /^https?:\/\/.+/;
      expect(urlRegex.test('https://example.com/jobs?id=123')).toBe(true);
    });
  });

  describe('Salary String Validation', () => {
    it('should accept various salary formats', () => {
      const salaries = [
        '40k-50k €',
        '40000-50000 EUR',
        'Selon profil',
        'À négocier',
        '45k€'
      ];

      salaries.forEach(salary => {
        expect(salary.length).toBeGreaterThan(0);
        expect(salary.length).toBeLessThanOrEqual(100);
      });
    });

    it('should reject salary exceeding max length', () => {
      const salary = 'A'.repeat(101);
      const isValid = salary.length <= 100;
      expect(isValid).toBe(false);
    });

    it('should accept salary at max length', () => {
      const salary = 'A'.repeat(100);
      const isValid = salary.length <= 100;
      expect(isValid).toBe(true);
    });
  });

  describe('Title Validation', () => {
    it('should accept title at max length', () => {
      const title = 'A'.repeat(200);
      const isValid = title.length > 0 && title.length <= 200;
      expect(isValid).toBe(true);
    });

    it('should reject title exceeding max length', () => {
      const title = 'A'.repeat(201);
      const isValid = title.length > 0 && title.length <= 200;
      expect(isValid).toBe(false);
    });

    it('should reject empty title', () => {
      const title = '';
      const isValid = title.length > 0 && title.length <= 200;
      expect(isValid).toBe(false);
    });

    it('should accept valid title', () => {
      const title = 'Développeur Full Stack';
      const isValid = title.length > 0 && title.length <= 200;
      expect(isValid).toBe(true);
    });
  });

  describe('Location Validation', () => {
    it('should accept location at max length', () => {
      const location = 'A'.repeat(100);
      const isValid = location.length > 0 && location.length <= 100;
      expect(isValid).toBe(true);
    });

    it('should reject location exceeding max length', () => {
      const location = 'A'.repeat(101);
      const isValid = location.length > 0 && location.length <= 100;
      expect(isValid).toBe(false);
    });

    it('should reject empty location', () => {
      const location = '';
      const isValid = location.length > 0 && location.length <= 100;
      expect(isValid).toBe(false);
    });

    it('should accept valid location', () => {
      const location = 'Paris, France';
      const isValid = location.length > 0 && location.length <= 100;
      expect(isValid).toBe(true);
    });
  });

  describe('URL Max Length Validation', () => {
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
      const url = 'https://example.com';
      const isValid = url.length <= 500;
      expect(isValid).toBe(true);
    });
  });

  describe('Form Reset', () => {
    it('should reset form to default values', () => {
      const defaultForm = {
        title: '',
        location: '',
        url: '',
        publication_date: new Date().toISOString().split('T')[0],
        end_date: null,
        experience_years: null,
        salary: null
      };

      expect(defaultForm.title).toBe('');
      expect(defaultForm.location).toBe('');
      expect(defaultForm.url).toBe('');
      expect(defaultForm.end_date).toBeNull();
      expect(defaultForm.experience_years).toBeNull();
      expect(defaultForm.salary).toBeNull();
      expect(defaultForm.publication_date).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });
  });

  describe('Date Comparison', () => {
    it('should validate end_date is after publication_date', () => {
      const publicationDate = new Date('2025-01-15');
      const endDate = new Date('2025-12-31');
      const isValid = endDate > publicationDate;
      expect(isValid).toBe(true);
    });

    it('should detect invalid end_date before publication_date', () => {
      const publicationDate = new Date('2025-12-31');
      const endDate = new Date('2025-01-15');
      const isValid = endDate > publicationDate;
      expect(isValid).toBe(false);
    });

    it('should detect equal dates', () => {
      const publicationDate = new Date('2025-01-15');
      const endDate = new Date('2025-01-15');
      const isValid = endDate > publicationDate;
      expect(isValid).toBe(false);
    });
  });

  describe('Job Offer Data Construction', () => {
    it('should construct complete job offer object with ISO datetime', () => {
      const formData = {
        title: 'Développeur Full Stack',
        location: 'Paris, France',
        url: 'https://example.com/jobs/123',
        publication_date: '2025-01-15',
        end_date: '2025-12-31',
        experience_years: 5,
        salary: '40k-50k €'
      };

      const jobData = {
        title: formData.title,
        location: formData.location,
        url: formData.url,
        publication_date: formData.publication_date + 'T00:00:00',
        end_date: formData.end_date ? formData.end_date + 'T23:59:59' : null,
        experience_years: formData.experience_years || null,
        salary: formData.salary || null
      };

      expect(jobData.title).toBe('Développeur Full Stack');
      expect(jobData.location).toBe('Paris, France');
      expect(jobData.url).toBe('https://example.com/jobs/123');
      expect(jobData.publication_date).toBe('2025-01-15T00:00:00');
      expect(jobData.end_date).toBe('2025-12-31T23:59:59');
      expect(jobData.experience_years).toBe(5);
      expect(jobData.salary).toBe('40k-50k €');
    });

    it('should construct minimal job offer object', () => {
      const formData = {
        title: 'Développeur',
        location: 'Paris',
        url: 'https://example.com',
        publication_date: '2025-01-15',
        end_date: '',
        experience_years: 0,
        salary: ''
      };

      const jobData = {
        title: formData.title,
        location: formData.location,
        url: formData.url,
        publication_date: formData.publication_date + 'T00:00:00',
        end_date: formData.end_date ? formData.end_date + 'T23:59:59' : null,
        experience_years: formData.experience_years || null,
        salary: formData.salary || null
      };

      expect(jobData.title).toBe('Développeur');
      expect(jobData.location).toBe('Paris');
      expect(jobData.url).toBe('https://example.com');
      expect(jobData.publication_date).toBe('2025-01-15T00:00:00');
      expect(jobData.end_date).toBeNull();
      expect(jobData.experience_years).toBeNull();
      expect(jobData.salary).toBeNull();
    });
  });

  describe('DateTime Format Conversion', () => {
    it('should convert date to ISO datetime for publication_date', () => {
      const date = '2025-01-15';
      const datetime = date + 'T00:00:00';
      expect(datetime).toBe('2025-01-15T00:00:00');
    });

    it('should convert date to ISO datetime for end_date', () => {
      const date = '2025-12-31';
      const datetime = date + 'T23:59:59';
      expect(datetime).toBe('2025-12-31T23:59:59');
    });

    it('should handle null end_date', () => {
      const date: string | null = null;
      const datetime = date ? date + 'T23:59:59' : null;
      expect(datetime).toBeNull();
    });

    it('should handle empty end_date', () => {
      const date = '';
      const datetime = date ? date + 'T23:59:59' : null;
      expect(datetime).toBeNull();
    });

    it('should validate ISO datetime format', () => {
      const datetime = '2025-01-15T00:00:00';
      const isoRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/;
      expect(isoRegex.test(datetime)).toBe(true);
    });

    it('should validate ISO datetime with end of day', () => {
      const datetime = '2025-12-31T23:59:59';
      const isoRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/;
      expect(isoRegex.test(datetime)).toBe(true);
    });
  });
});
