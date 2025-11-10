import type { SponsoringOption } from "~/utils/api";

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
  const getOptionName = (option: SponsoringOption): string => {
    return option.name || 'Option sans nom';
  };

  /**
   * Récupère la description de l'option
   * @param option - L'option de sponsoring
   * @returns La description traduite ou null
   */
  const getOptionDescription = (option: SponsoringOption): string | null => {
    return option.description || null;
  };

  return {
    getOptionName,
    getOptionDescription
  };
};
