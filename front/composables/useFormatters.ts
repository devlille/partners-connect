/**
 * Composable pour le formatage localisé des dates et nombres
 *
 * Utilise les formats définis dans nuxt.config.ts (datetimeFormats et numberFormats)
 * pour assurer une cohérence dans toute l'application.
 *
 * @example
 * ```typescript
 * const { formatDate, formatCurrency, formatNumber, formatPercent } = useFormatters()
 *
 * formatDate(new Date(), 'short')  // "25/10/2025"
 * formatDate(new Date(), 'long')   // "25 octobre 2025"
 * formatCurrency(1234.56)          // "1 234,56 €"
 * formatNumber(1234.567, 2)        // "1 234,57"
 * formatPercent(0.856)             // "85,6 %"
 * ```
 */
export function useFormatters() {
  const { d, n } = useI18n();

  /**
   * Formate une date selon le format spécifié
   *
   * @param date - Date à formater (Date, string ISO, ou timestamp)
   * @param format - Format: 'short' (25/10/2025), 'long' (25 octobre 2025), 'full' (samedi 25 octobre 2025)
   * @returns Date formatée selon la locale active
   *
   * @example
   * formatDate(new Date('2025-10-25'), 'short')  // "25/10/2025"
   * formatDate('2025-10-25T14:30:00Z', 'long')   // "25 octobre 2025"
   * formatDate(1729862400000, 'full')            // "samedi 25 octobre 2025"
   */
  function formatDate(
    date: Date | string | number,
    format: "short" | "long" | "full" = "short",
  ): string {
    if (!date) return "";

    try {
      const dateObj = typeof date === "string" || typeof date === "number" ? new Date(date) : date;

      // Vérifier que la date est valide
      if (isNaN(dateObj.getTime())) {
        console.warn("Invalid date:", date);
        return "";
      }

      return d(dateObj, format);
    } catch (error) {
      console.error("Error formatting date:", error, date);
      return "";
    }
  }

  /**
   * Formate un prix en devise locale
   *
   * @param amount - Montant à formater
   * @param options - Options supplémentaires (currency, notation, etc.)
   * @returns Montant formaté avec symbole de devise
   *
   * @example
   * formatCurrency(1234.56)                    // "1 234,56 €" (fr)
   * formatCurrency(1234.56, { currency: 'USD' })  // "$1,234.56" (en)
   */
  function formatCurrency(
    amount: number | string,
    options?: Partial<Intl.NumberFormatOptions>,
  ): string {
    if (amount === null || amount === undefined || amount === "") return "";

    try {
      const numericAmount = typeof amount === "string" ? parseFloat(amount) : amount;

      if (isNaN(numericAmount)) {
        console.warn("Invalid currency amount:", amount);
        return "";
      }

      return n(numericAmount, {
        ...options,
        key: "currency",
      });
    } catch (error) {
      console.error("Error formatting currency:", error, amount);
      return "";
    }
  }

  /**
   * Formate un nombre avec séparateurs de milliers
   *
   * @param value - Nombre à formater
   * @param decimals - Nombre de décimales (optionnel)
   * @returns Nombre formaté selon la locale
   *
   * @example
   * formatNumber(1234.567)       // "1 234,57" (fr)
   * formatNumber(1234.567, 0)    // "1 235" (fr)
   * formatNumber(1234.567, 3)    // "1 234,567" (fr)
   */
  function formatNumber(value: number | string, decimals?: number): string {
    if (value === null || value === undefined || value === "") return "";

    try {
      const numericValue = typeof value === "string" ? parseFloat(value) : value;

      if (isNaN(numericValue)) {
        console.warn("Invalid number:", value);
        return "";
      }

      const options: Intl.NumberFormatOptions = {
        style: "decimal",
        useGrouping: true,
      };

      if (decimals !== undefined) {
        options.minimumFractionDigits = decimals;
        options.maximumFractionDigits = decimals;
      }

      return n(numericValue, options);
    } catch (error) {
      console.error("Error formatting number:", error, value);
      return "";
    }
  }

  /**
   * Formate un pourcentage
   *
   * @param value - Valeur décimale (0.856 pour 85.6%)
   * @param decimals - Nombre de décimales (défaut: 1)
   * @returns Pourcentage formaté
   *
   * @example
   * formatPercent(0.856)       // "85,6 %" (fr)
   * formatPercent(0.8567, 2)   // "85,67 %" (fr)
   * formatPercent(1)           // "100 %" (fr)
   */
  function formatPercent(value: number | string, decimals = 1): string {
    if (value === null || value === undefined || value === "") return "";

    try {
      const numericValue = typeof value === "string" ? parseFloat(value) : value;

      if (isNaN(numericValue)) {
        console.warn("Invalid percent value:", value);
        return "";
      }

      return n(numericValue, {
        style: "percent",
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
      });
    } catch (error) {
      console.error("Error formatting percent:", error, value);
      return "";
    }
  }

  /**
   * Formate une durée en format humain
   *
   * @param milliseconds - Durée en millisecondes
   * @returns Durée formatée (ex: "2 min", "1h 30min", "3j")
   *
   * @example
   * formatDuration(60000)       // "1 min"
   * formatDuration(5400000)     // "1h 30min"
   * formatDuration(172800000)   // "2j"
   */
  function formatDuration(milliseconds: number): string {
    if (!milliseconds || milliseconds < 0) return "0s";

    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) {
      const remainingHours = hours % 24;
      return remainingHours > 0 ? `${days}j ${remainingHours}h` : `${days}j`;
    }

    if (hours > 0) {
      const remainingMinutes = minutes % 60;
      return remainingMinutes > 0 ? `${hours}h ${remainingMinutes}min` : `${hours}h`;
    }

    if (minutes > 0) {
      const remainingSeconds = seconds % 60;
      return remainingSeconds > 0 ? `${minutes}min ${remainingSeconds}s` : `${minutes}min`;
    }

    return `${seconds}s`;
  }

  /**
   * Formate une date relative (ex: "il y a 2 heures")
   *
   * @param date - Date à comparer avec maintenant
   * @returns Texte relatif selon la locale
   *
   * @example
   * formatRelativeTime(new Date(Date.now() - 3600000))  // "il y a 1 heure"
   * formatRelativeTime(new Date(Date.now() + 86400000)) // "dans 1 jour"
   */
  function formatRelativeTime(date: Date | string | number): string {
    if (!date) return "";

    try {
      const dateObj = typeof date === "string" || typeof date === "number" ? new Date(date) : date;

      if (isNaN(dateObj.getTime())) {
        console.warn("Invalid date:", date);
        return "";
      }

      const now = new Date();
      const diffMs = dateObj.getTime() - now.getTime();
      const diffSeconds = Math.floor(diffMs / 1000);
      const diffMinutes = Math.floor(diffSeconds / 60);
      const diffHours = Math.floor(diffMinutes / 60);
      const diffDays = Math.floor(diffHours / 24);

      const rtf = new Intl.RelativeTimeFormat(useI18n().locale.value, { numeric: "auto" });

      if (Math.abs(diffDays) >= 1) {
        return rtf.format(diffDays, "day");
      }
      if (Math.abs(diffHours) >= 1) {
        return rtf.format(diffHours, "hour");
      }
      if (Math.abs(diffMinutes) >= 1) {
        return rtf.format(diffMinutes, "minute");
      }
      return rtf.format(diffSeconds, "second");
    } catch (error) {
      console.error("Error formatting relative time:", error, date);
      return "";
    }
  }

  /**
   * Formate une taille de fichier
   *
   * @param bytes - Taille en octets
   * @param decimals - Nombre de décimales (défaut: 2)
   * @returns Taille formatée (ex: "1,5 MB", "256 KB")
   *
   * @example
   * formatFileSize(1536)           // "1,50 KB"
   * formatFileSize(1048576)        // "1,00 MB"
   * formatFileSize(5242880, 1)     // "5,0 MB"
   */
  function formatFileSize(bytes: number, decimals = 2): string {
    if (bytes === 0) return "0 B";
    if (!bytes || bytes < 0) return "";

    const k = 1024;
    const sizes = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    const value = bytes / Math.pow(k, i);

    return `${formatNumber(value, decimals)} ${sizes[i]}`;
  }

  return {
    formatDate,
    formatCurrency,
    formatNumber,
    formatPercent,
    formatDuration,
    formatRelativeTime,
    formatFileSize,
  };
}
