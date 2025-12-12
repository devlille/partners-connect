/**
 * Constantes de validation réutilisables
 */

export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const URL_REGEX = /^https?:\/\/.+/;

export const SIRET_REGEX = /^\d{14}$/;

// Format EU: 2 lettres (code pays) + 8-12 caractères alphanumériques
export const VAT_REGEX = /^[A-Z]{2}[A-Z0-9]{8,12}$/;

export const ZIP_CODE_REGEX = /^\d{5}$/;

/**
 * Messages d'erreur de validation
 */
export const VALIDATION_MESSAGES = {
  EMAIL_INVALID: "Adresse email invalide",
  REQUIRED: "Ce champ est requis",
  URL_INVALID: "URL invalide",
  SIRET_INVALID: "Numéro SIRET invalide (14 chiffres)",
  VAT_INVALID: "Numéro de TVA invalide",
  ZIP_CODE_INVALID: "Code postal invalide (5 chiffres)",
} as const;
