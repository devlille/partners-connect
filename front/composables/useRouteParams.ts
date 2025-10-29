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

  return {
    getParam,
    getOrgSlug,
    getEventSlug
  };
};
