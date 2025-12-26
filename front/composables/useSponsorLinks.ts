import { computed, type Ref } from "vue";
import type { ExtendedPartnershipItem } from "~/types/partnership";

interface SponsorLinksOptions {
  orgSlug: string;
  eventSlug: string;
  sponsorId: string;
  partnership?: Ref<ExtendedPartnershipItem | null>;
  company?: Ref<any | null>;
  billing?: Ref<any | null>;
}

export const useSponsorLinks = (
  orgSlugOrOptions: string | SponsorLinksOptions,
  eventSlug?: string,
  sponsorId?: string,
) => {
  // Support both old API (3 strings) and new API (options object)
  const options: SponsorLinksOptions =
    typeof orgSlugOrOptions === "string"
      ? {
          orgSlug: orgSlugOrOptions,
          eventSlug: eventSlug!,
          sponsorId: sponsorId!,
        }
      : orgSlugOrOptions;

  const { isPartnershipComplete, isCompanyComplete, isBillingComplete } = usePartnershipValidation();

  const sponsorLinks = computed(() => {
    const basePath = `/orgs/${options.orgSlug}/events/${options.eventSlug}/sponsors/${options.sponsorId}`;

    const partnershipComplete = options.partnership
      ? isPartnershipComplete(options.partnership.value)
      : true;
    const companyComplete = options.company ? isCompanyComplete(options.company.value) : true;
    const billingComplete = options.billing ? isBillingComplete(options.billing.value) : true;

    // La page "Partenariat" affiche à la fois les infos du partenariat et de facturation
    const partnershipPageComplete = partnershipComplete && billingComplete;

    return [
      {
        label: "Retour aux sponsors",
        icon: "i-heroicons-arrow-left",
        to: `/orgs/${options.orgSlug}/events/${options.eventSlug}/sponsors`,
      },
      {
        label: "Partenariat",
        icon: "i-heroicons-hand-raised",
        to: basePath,
        badge: !partnershipPageComplete
          ? {
              label: "!",
              color: "error" as const,
              title: "Informations incomplètes",
            }
          : undefined,
      },
      {
        label: "Entreprise",
        icon: "i-heroicons-building-office",
        to: `${basePath}/company`,
        badge: !companyComplete
          ? {
              label: "!",
              color: "error" as const,
              title: "Informations incomplètes",
            }
          : undefined,
      },
      {
        label: "Tickets",
        icon: "i-heroicons-ticket",
        to: `${basePath}/tickets`,
      },
      {
        label: "Communication",
        icon: "i-heroicons-megaphone",
        to: `${basePath}/communication`,
      },
      {
        label: "Documents",
        icon: "i-heroicons-document-text",
        to: `${basePath}/documents`,
      },
      {
        label: "Offres d'emploi",
        icon: "i-heroicons-briefcase",
        to: `${basePath}/job-offers`,
      },
      {
        label: "Liens externes",
        icon: "i-heroicons-link",
        to: `${basePath}/external-links`,
      },
    ];
  });

  return { sponsorLinks };
};
