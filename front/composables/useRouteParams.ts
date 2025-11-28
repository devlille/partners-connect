/**
 * Composable pour extraire et normaliser les paramètres de route
 */
export const useRouteParams = () => {
  const route = useRoute();

  /**
   * Extrait un paramètre de route et le convertit en string
   * @param paramName - Nom du paramètre (ex: 'slug', 'eventSlug')
   * @param lowercase - Convertir en minuscules (défaut: false)
   */
  function getParam(paramName: string, lowercase: boolean = false): string {
    const params = route.params[paramName];
    let value = Array.isArray(params) ? params[0] : params;

    if (typeof value !== 'string') {
      value = String(value);
    }

    return lowercase ? value.toLowerCase() : value;
  }

  /**
   * Extrait le slug de l'organisation
   * @param lowercase - Convertir en minuscules (défaut: false)
   */
  function getOrgSlug(lowercase: boolean = false): string {
    return getParam('slug', lowercase);
  }

  /**
   * Extrait le slug de l'événement
   * @param lowercase - Convertir en minuscules (défaut: false)
   */
  function getEventSlug(lowercase: boolean = false): string {
    return getParam('eventSlug', lowercase);
  }

  /**
   * Extrait l'ID du sponsor/partenariat
   */
  function getSponsorId(): string {
    return getParam('sponsorId');
  }

  /**
   * Extrait l'ID du partenariat
   */
  function getPartnershipId(): string {
    return getParam('partnershipId');
  }

  /**
   * Retourne des refs computed pour orgSlug et eventSlug
   * Utile pour l'usage avec watch ou composables réactifs
   */
  const orgSlug = computed(() => getOrgSlug());
  const eventSlug = computed(() => getEventSlug());
  const sponsorId = computed(() => getSponsorId());
  const partnershipId = computed(() => getPartnershipId());

  return {
    getParam,
    getOrgSlug,
    getEventSlug,
    getSponsorId,
    getPartnershipId,
    orgSlug,
    eventSlug,
    sponsorId,
    partnershipId
  };
};
