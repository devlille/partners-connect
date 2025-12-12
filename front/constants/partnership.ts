/**
 * Messages d'erreur pour les actions de partenariat
 */
export const PARTNERSHIP_ACTION_ERRORS = {
  /** Partenariat non trouvé (404) */
  NOT_FOUND: "Partenariat introuvable",
  /** Action non autorisée (403) */
  FORBIDDEN: "Vous n'êtes pas autorisé à effectuer cette action",
  /** Erreur générique lors de la validation */
  VALIDATION_GENERIC: "Une erreur est survenue lors de la validation du partenariat",
  /** Erreur générique lors du refus */
  DECLINE_GENERIC: "Une erreur est survenue lors du refus du partenariat",
} as const;

/**
 * Messages de succès pour les actions de partenariat
 */
export const PARTNERSHIP_ACTION_SUCCESS = {
  /** Message de succès après validation */
  VALIDATED: (companyName: string) => `Le partenariat avec ${companyName} a été validé avec succès`,
  /** Message de succès après refus */
  DECLINED: (companyName: string) => `Le partenariat avec ${companyName} a été refusé`,
} as const;

/**
 * Titres et messages de confirmation
 */
export const PARTNERSHIP_CONFIRM = {
  VALIDATE: {
    title: "Valider le partenariat",
    message: (companyName: string) =>
      `Êtes-vous sûr de vouloir valider le partenariat avec ${companyName} ?`,
    confirmLabel: "Valider",
    type: "info" as const,
  },
  DECLINE: {
    title: "Refuser le partenariat",
    message: (companyName: string) =>
      `Êtes-vous sûr de vouloir refuser le partenariat avec ${companyName} ?`,
    confirmLabel: "Refuser",
    type: "danger" as const,
  },
} as const;
