import type { ExtendedPartnershipItem } from "~/types/partnership";

export const usePartnershipValidation = () => {
  // Regex pour valider les emails
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  // Regex pour valider les numéros de téléphone (accepte +, chiffres, espaces, tirets, parenthèses)
  const phoneRegex = /^[\d\s\-+()]+$/;

  /**
   * Vérifie si un email est valide
   */
  function isValidEmail(email: string): boolean {
    return emailRegex.test(email.trim());
  }

  /**
   * Vérifie si tous les emails d'une chaîne séparée par des virgules sont valides
   */
  function areEmailsValid(emails: string | null | undefined): boolean {
    if (!emails) return false;
    const emailList = emails
      .split(",")
      .map((e) => e.trim())
      .filter((e) => e.length > 0);
    if (emailList.length === 0) return false;
    return emailList.every((email) => isValidEmail(email));
  }

  /**
   * Vérifie si le numéro de téléphone est valide
   */
  function isPhoneValid(phone: string | null | undefined): boolean {
    if (!phone) return false;
    const phoneStr = phone.trim();
    // Doit contenir au moins 8 chiffres et respecter le format
    const digitsOnly = phoneStr.replace(/[^\d]/g, "");
    return phoneRegex.test(phoneStr) && digitsOnly.length >= 8;
  }

  /**
   * Vérifie si les informations du partenariat sont complètes
   */
  function isPartnershipComplete(partnership: ExtendedPartnershipItem | null): boolean {
    if (!partnership) return false;

    return !!(
      partnership.contact?.display_name &&
      partnership.contact?.role &&
      partnership.language &&
      partnership.emails &&
      areEmailsValid(partnership.emails) &&
      partnership.phone &&
      isPhoneValid(partnership.phone)
    );
  }

  /**
   * Vérifie si les informations de l'entreprise sont complètes
   */
  function isCompanyComplete(company: any | null): boolean {
    if (!company) return false;

    return !!(
      company.siret &&
      company.site_url &&
      company.head_office?.address &&
      company.head_office?.city &&
      company.head_office?.zip_code &&
      company.head_office?.country
    );
  }

  /**
   * Vérifie si les informations de facturation sont complètes
   */
  function isBillingComplete(billing: any | null): boolean {
    if (!billing) return false;

    return !!(
      billing.contact?.first_name &&
      billing.contact?.last_name &&
      billing.contact?.email &&
      isValidEmail(billing.contact.email)
    );
  }

  /**
   * Vérifie si toutes les informations requises sont complètes
   */
  function isAllDataComplete(
    partnership: ExtendedPartnershipItem | null,
    company: any | null,
    billing?: any | null,
  ): boolean {
    const partnershipOk = isPartnershipComplete(partnership);
    const companyOk = isCompanyComplete(company);
    const billingOk = billing !== undefined ? isBillingComplete(billing) : true;
    return partnershipOk && companyOk && billingOk;
  }

  return {
    isPartnershipComplete,
    isCompanyComplete,
    isBillingComplete,
    isAllDataComplete,
  };
};
