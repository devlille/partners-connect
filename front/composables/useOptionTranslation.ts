import type { SponsoringOption, DefinitionsTranslatedLabel } from "~/utils/api";

// Type pour les traductions sous forme d'objet (dictionnaire)
type TranslationsDict = Record<string, DefinitionsTranslatedLabel>;

/**
 * Composable pour gérer les traductions des options de sponsoring
 * Centralise la logique d'extraction du nom traduit
 */
export const useOptionTranslation = () => {
  /**
   * Récupère le nom de l'option
   * @param option - L'option de sponsoring
   * @returns Le nom traduit ou un fallback
   */
  const getOptionName = (
    option: SponsoringOption | (SponsoringOption & { translations?: TranslationsDict }),
  ): string => {
    // Si l'option a des traductions (objet avec langues comme clés)
    if (
      "translations" in option &&
      option.translations &&
      typeof option.translations === "object"
    ) {
      // Récupérer la première traduction disponible
      const translationValues = Object.values(option.translations);
      if (translationValues.length > 0) {
        const firstTranslation = translationValues[0];
        if (firstTranslation?.name) {
          return firstTranslation.name;
        }
      }
    }

    // Sinon, utiliser le champ name direct
    return option.name || "Option sans nom";
  };

  /**
   * Récupère la description de l'option
   * @param option - L'option de sponsoring
   * @returns La description traduite ou null
   */
  const getOptionDescription = (
    option: SponsoringOption | (SponsoringOption & { translations?: TranslationsDict }),
  ): string | null => {
    // Si l'option a des traductions (objet avec langues comme clés)
    if (
      "translations" in option &&
      option.translations &&
      typeof option.translations === "object"
    ) {
      // Récupérer la première traduction disponible
      const translationValues = Object.values(option.translations);
      if (translationValues.length > 0) {
        const firstTranslation = translationValues[0];
        if (firstTranslation?.description) {
          return firstTranslation.description;
        }
      }
    }

    // Sinon, utiliser le champ description direct
    return option.description || null;
  };

  return {
    getOptionName,
    getOptionDescription,
  };
};
