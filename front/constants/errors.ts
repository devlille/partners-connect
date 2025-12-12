/**
 * Messages d'erreur standardisés
 * Centralise tous les messages pour faciliter l'i18n et la cohérence
 */

export const ERROR_MESSAGES = {
  // Generic
  GENERIC: "Une erreur est survenue. Veuillez réessayer.",
  NETWORK: "Erreur de connexion. Vérifiez votre connexion internet.",
  UNAUTHORIZED: "Vous n'êtes pas autorisé à effectuer cette action.",
  NOT_FOUND: "La ressource demandée est introuvable.",
  SERVER_ERROR: "Erreur serveur. Veuillez réessayer plus tard.",

  // Validation
  REQUIRED_FIELD: "Ce champ est obligatoire",
  INVALID_EMAIL: "L'adresse email n'est pas valide",
  INVALID_URL: "L'URL n'est pas valide",
  INVALID_DATE: "La date n'est pas valide",
  MIN_LENGTH: (min: number) => `Minimum ${min} caractères requis`,
  MAX_LENGTH: (max: number) => `Maximum ${max} caractères autorisés`,
  MIN_VALUE: (min: number) => `La valeur minimale est ${min}`,
  MAX_VALUE: (max: number) => `La valeur maximale est ${max}`,

  // API Operations
  LOAD_FAILED: (resource: string) => `Impossible de charger ${resource}`,
  CREATE_FAILED: (resource: string) => `Impossible de créer ${resource}`,
  UPDATE_FAILED: (resource: string) => `Impossible de mettre à jour ${resource}`,
  DELETE_FAILED: (resource: string) => `Impossible de supprimer ${resource}`,

  // Specific Resources
  COMPANY_NOT_FOUND: "Entreprise introuvable",
  EVENT_NOT_FOUND: "Événement introuvable",
  SPONSOR_NOT_FOUND: "Sponsor introuvable",
  PACK_NOT_FOUND: "Pack introuvable",
  OPTION_NOT_FOUND: "Option introuvable",
  PROVIDER_NOT_FOUND: "Prestataire introuvable",

  // Form Fields
  NAME_REQUIRED: "Le nom est obligatoire",
  TITLE_REQUIRED: "Le titre est obligatoire",
  TYPE_REQUIRED: "Le type est obligatoire",
  LOCATION_REQUIRED: "La localisation est obligatoire",
  URL_REQUIRED: "L'URL est obligatoire",
  DATE_REQUIRED: "La date est obligatoire",
  STREET_REQUIRED: "La rue est obligatoire",
  CITY_REQUIRED: "La ville est obligatoire",
  ZIP_REQUIRED: "Le code postal est obligatoire",
  COUNTRY_REQUIRED: "Le pays est obligatoire",
  SIRET_REQUIRED: "Le SIRET est obligatoire",
  VAT_REQUIRED: "La TVA est obligatoire",
  WEBSITE_REQUIRED: "Le site web est obligatoire",
  DESCRIPTION_REQUIRED: "La description est obligatoire",
  PUBLICATION_DATE_REQUIRED: "La date de publication est obligatoire",
} as const;

/**
 * Messages de succès standardisés
 */
export const SUCCESS_MESSAGES = {
  GENERIC: "Opération réussie",
  CREATED: (resource: string) => `${resource} créé avec succès`,
  UPDATED: (resource: string) => `${resource} mis à jour avec succès`,
  DELETED: (resource: string) => `${resource} supprimé avec succès`,
  SAVED: "Modifications enregistrées",
} as const;

/**
 * Messages de confirmation
 */
export const CONFIRMATION_MESSAGES = {
  DELETE: (resource: string, name?: string) =>
    name
      ? `Êtes-vous sûr de vouloir supprimer ${resource} "${name}" ?`
      : `Êtes-vous sûr de vouloir supprimer ce ${resource} ?`,
  DELETE_TITLE: (resource: string) => `Confirmer la suppression du ${resource}`,
  DELETE_MESSAGE: (resource: string, name: string) =>
    `Êtes-vous sûr de vouloir supprimer le ${resource} <strong>${name}</strong> ?`,
  UNSAVED_CHANGES:
    "Vous avez des modifications non sauvegardées. Voulez-vous quitter sans enregistrer ?",
  IRREVERSIBLE_ACTION: "Cette action est irréversible.",
} as const;
