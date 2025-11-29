/**
 * Composable pour récupérer la configuration de l'organisation et de l'événement
 * depuis les query parameters ou les valeurs par défaut
 */
export const useEventConfig = () => {
  const route = useRoute();
  const config = useRuntimeConfig();

  /**
   * Récupère le slug de l'organisation depuis les query params ou la config par défaut
   */
  const orgSlug = computed(() => {
    const queryParam = route.query.orgSlug;
    if (queryParam) {
      return Array.isArray(queryParam) ? queryParam[0] : queryParam;
    }
    return config.public.defaultOrgSlug as string;
  });

  /**
   * Récupère le slug de l'événement depuis les query params ou la config par défaut
   */
  const eventSlug = computed(() => {
    const queryParam = route.query.eventSlug;
    if (queryParam) {
      return Array.isArray(queryParam) ? queryParam[0] : queryParam;
    }
    return config.public.defaultEventSlug as string;
  });

  return {
    orgSlug,
    eventSlug,
  };
};
