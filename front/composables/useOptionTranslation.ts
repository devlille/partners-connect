import type { SponsoringOption } from "~/utils/api";

/**
 * Composable pour gérer les traductions des options de sponsoring
 * Centralise la logique d'extraction du nom traduit
 */
export const useOptionTranslation = () => {
  /**
   * Récupère le nom de l'option dans la première traduction disponible
   * @param option - L'option de sponsoring
   * @returns Le nom traduit ou un fallback
   */
  const getOptionName = (option: SponsoringOption): string => {
    // Récupérer la première traduction disponible
    if (option.translations) {
      const firstTranslation = Object.values(option.translations)[0];
      if (firstTranslation && typeof firstTranslation === 'object' && 'name' in firstTranslation) {
        return firstTranslation.name as string;
      }
    }
    // Fallback sur le nom direct ou message par défaut
    return option.name || 'Option sans nom';
  };

  /**
   * Récupère la description de l'option dans la première traduction disponible
   * @param option - L'option de sponsoring
   * @returns La description traduite ou null
   */
  const getOptionDescription = (option: SponsoringOption): string | null => {
    if (option.translations) {
      const firstTranslation = Object.values(option.translations)[0];
      if (firstTranslation && typeof firstTranslation === 'object' && 'description' in firstTranslation) {
        return firstTranslation.description as string;
      }
    }
    return option.description || null;
  };

  return {
    getOptionName,
    getOptionDescription
  };
};
