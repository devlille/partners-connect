/**
 * Shared validation schema for partnership forms
 *
 * These validation rules are shared between authenticated and public pages
 * to ensure consistent validation behavior across the application.
 *
 * Feature: 001-public-partnership-view
 * Created: 2025-11-18
 */

/**
 * Validation constraints for partnership fields
 * These rules must match backend validation exactly
 */
export const VALIDATION_RULES = {
  contact_name: {
    required: true,
    minLength: 2,
    maxLength: 100,
    message: 'Le nom du contact doit contenir entre 2 et 100 caractères'
  },
  contact_role: {
    required: true,
    minLength: 2,
    maxLength: 50,
    message: 'Le rôle du contact doit contenir entre 2 et 50 caractères'
  },
  email: {
    required: true,
    pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
    message: 'Format d\'email invalide'
  },
  phone: {
    required: false,
    pattern: /^\+?[0-9\s\-()]+$/,
    message: 'Format de téléphone invalide'
  },
  company_name: {
    required: true,
    minLength: 2,
    maxLength: 200,
    message: 'Le nom de l\'entreprise doit contenir entre 2 et 200 caractères'
  },
  website: {
    required: false,
    pattern: /^https?:\/\/.+/,
    message: 'URL invalide (doit commencer par http:// ou https://)'
  },
  language: {
    required: true,
    enum: ['fr', 'en', 'es'] as const,
    message: 'Langue invalide (fr, en, ou es requis)'
  }
} as const;

/**
 * Validation result type
 */
export interface ValidationResult {
  valid: boolean;
  errors: Record<string, string>;
}

/**
 * Validate a single field value against its validation rule
 *
 * @param fieldName - Field name from VALIDATION_RULES
 * @param value - Field value to validate
 * @returns Validation error message or null if valid
 */
export function validateField(
  fieldName: keyof typeof VALIDATION_RULES,
  value: unknown
): string | null {
  const rule = VALIDATION_RULES[fieldName];

  // Required field validation
  if (rule.required && (value === null || value === undefined || value === '')) {
    return rule.message || `Le champ ${fieldName} est requis`;
  }

  // Skip other validations if field is optional and empty
  if (!rule.required && (value === null || value === undefined || value === '')) {
    return null;
  }

  const stringValue = String(value);

  // Min length validation
  if ('minLength' in rule && stringValue.length < rule.minLength) {
    return rule.message || `Le champ doit contenir au moins ${rule.minLength} caractères`;
  }

  // Max length validation
  if ('maxLength' in rule && stringValue.length > rule.maxLength) {
    return rule.message || `Le champ ne doit pas dépasser ${rule.maxLength} caractères`;
  }

  // Pattern validation
  if ('pattern' in rule && !rule.pattern.test(stringValue)) {
    return rule.message || `Format invalide`;
  }

  // Enum validation
  if ('enum' in rule && !rule.enum.includes(value as any)) {
    return rule.message || `Valeur invalide`;
  }

  return null;
}

/**
 * Validate partnership contact information
 *
 * @param data - Partnership contact data
 * @returns Validation result with errors if any
 */
export function validatePartnershipData(data: {
  contact_name?: string;
  contact_role?: string;
  phone?: string | null;
  emails?: string;
  language?: string;
}): ValidationResult {
  const errors: Record<string, string> = {};

  // Validate contact name
  const contactNameError = validateField('contact_name', data.contact_name);
  if (contactNameError) errors.contact_name = contactNameError;

  // Validate contact role
  const contactRoleError = validateField('contact_role', data.contact_role);
  if (contactRoleError) errors.contact_role = contactRoleError;

  // Validate phone (optional)
  const phoneError = validateField('phone', data.phone);
  if (phoneError) errors.phone = phoneError;

  // Validate email(s)
  if (data.emails) {
    const emailList = data.emails.split(',').map(e => e.trim());
    for (const email of emailList) {
      const emailError = validateField('email', email);
      if (emailError) {
        errors.emails = emailError;
        break;
      }
    }
  } else {
    errors.emails = 'Au moins une adresse email est requise';
  }

  // Validate language
  const languageError = validateField('language', data.language);
  if (languageError) errors.language = languageError;

  return {
    valid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Validate company information
 *
 * @param data - Company data
 * @returns Validation result with errors if any
 */
export function validateCompanyData(data: {
  name?: string;
  website?: string | null;
}): ValidationResult {
  const errors: Record<string, string> = {};

  // Validate company name
  const nameError = validateField('company_name', data.name);
  if (nameError) errors.name = nameError;

  // Validate website (optional)
  const websiteError = validateField('website', data.website);
  if (websiteError) errors.website = websiteError;

  return {
    valid: Object.keys(errors).length === 0,
    errors
  };
}

/**
 * Debounce validation for real-time feedback
 * Returns a debounced validation function that waits `delay` ms before executing
 *
 * @param validationFn - Validation function to debounce
 * @param delay - Delay in milliseconds (default: 300ms for <500ms requirement)
 * @returns Debounced validation function
 */
export function debounceValidation<T extends (...args: any[]) => any>(
  validationFn: T,
  delay: number = 300
): (...args: Parameters<T>) => Promise<ReturnType<T>> {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;

  return (...args: Parameters<T>): Promise<ReturnType<T>> => {
    return new Promise((resolve) => {
      if (timeoutId) clearTimeout(timeoutId);

      timeoutId = setTimeout(() => {
        resolve(validationFn(...args));
      }, delay);
    });
  };
}
