/**
 * Format a monetary integer as a currency string using the active i18n locale.
 *
 * @param value - Amount in major currency units (no decimals expected from the API)
 * @param currency - ISO 4217 currency code (e.g. "EUR") returned by the API
 * @param locale - BCP-47 locale; defaults to "fr-FR" (the app's default i18n locale)
 */
export function formatPrice(value: number, currency: string, locale: string = "fr-FR"): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(value);
}
