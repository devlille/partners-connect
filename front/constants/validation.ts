/**
 * Constantes de validation réutilisables
 */

export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const URL_REGEX = /^https?:\/\/.+/;

export const SIRET_REGEX = /^\d{14}$/;

export const VAT_REGEX = /^[A-Z]{2}\d{11}$/;

/**
 * Messages d'erreur de validation
 */
export const VALIDATION_MESSAGES = {
  EMAIL_INVALID: 'Adresse email invalide',
  REQUIRED: 'Ce champ est requis',
  URL_INVALID: 'URL invalide',
  SIRET_INVALID: 'Numéro SIRET invalide (14 chiffres)',
  VAT_INVALID: 'Numéro de TVA invalide'
} as const;
