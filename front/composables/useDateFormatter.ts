export const useDateFormatter = () => {
  /**
   * Formate une date ISO en format français lisible
   * @param dateString - Date au format ISO (ex: "2024-10-22T10:30:00Z")
   * @param options - Options de formatage Intl.DateTimeFormat
   * @returns Date formatée (ex: "22 octobre 2024")
   */
  const formatDate = (
    dateString: string,
    options: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }
  ): string => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', options).format(date);
  }

  /**
   * Formate une date ISO en format court français
   * @param dateString - Date au format ISO
   * @returns Date formatée (ex: "22/10/2024")
   */
  const formatDateShort = (dateString: string): string => {
    return formatDate(dateString, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  }

  /**
   * Formate une date ISO avec l'heure
   * @param dateString - Date au format ISO
   * @returns Date et heure formatées (ex: "22 octobre 2024 à 10:30")
   */
  const formatDateTime = (dateString: string): string => {
    return formatDate(dateString, {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  return {
    formatDate,
    formatDateShort,
    formatDateTime
  }
}
