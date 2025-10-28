import type { SponsoringOption } from '~/utils/api';

export const useSponsoringOptions = () => {
  const options = ref<SponsoringOption[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  /**
   * Charge les options de sponsoring pour un événement
   */
  async function loadOptions(orgSlug: string, eventSlug: string) {
    try {
      loading.value = true;
      error.value = null;

      const { getOrgsEventsOptions } = await import('~/utils/api');
      const response = await getOrgsEventsOptions(orgSlug, eventSlug);
      options.value = response.data;
    } catch (err) {
      console.error('Failed to load sponsoring options:', err);
      error.value = 'Impossible de charger les options';
    } finally {
      loading.value = false;
    }
  }

  /**
   * Récupère une option par son ID
   */
  function getOptionById(optionId: string): SponsoringOption | undefined {
    return options.value.find(opt => opt.id === optionId);
  }

  /**
   * Récupère le nom d'une option (avec traduction si disponible)
   */
  function getOptionName(optionId: string): string {
    const option = getOptionById(optionId);
    if (!option) return optionId;

    const { getOptionName: translateOptionName } = useOptionTranslation();
    return translateOptionName(option);
  }

  /**
   * Récupère les noms de plusieurs options
   */
  function getOptionNames(optionIds: string[]): string[] {
    return optionIds.map(id => getOptionName(id));
  }

  return {
    options,
    loading,
    error,
    loadOptions,
    getOptionById,
    getOptionName,
    getOptionNames
  };
};
