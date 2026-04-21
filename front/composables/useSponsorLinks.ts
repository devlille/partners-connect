import { computed, isRef, type Ref } from "vue";
import type { ExtendedPartnershipItem } from "~/types/partnership";

interface SponsorLinksOptions {
  orgSlug: string;
  eventSlug: string;
  sponsorId: string;
  partnership?: Ref<ExtendedPartnershipItem | null>;
  company?: Ref<any | null>;
  billing?: Ref<any | null>;
  qandaEnabled?: Ref<boolean> | boolean;
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

  const { isPartnershipComplete, isCompanyComplete, isBillingComplete } =
    usePartnershipValidation();

  const qandaActive = computed(() => {
    const val = options.qandaEnabled;
    if (!val) return false;
    return isRef(val) ? val.value : val;
  });

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
        label: "Activités",
        icon: "i-heroicons-calendar-days",
        to: `${basePath}/activities`,
      },
      {
        label: "Offres d'emploi",
        icon: "i-heroicons-briefcase",
        to: `${basePath}/job-offers`,
      },
      {
        label: "Speakers",
        icon: "i-heroicons-microphone",
        to: `${basePath}/speakers`,
      },
      {
        label: "Booth",
        icon: "i-heroicons-map-pin",
        to: `${basePath}/booth`,
      },
      {
        label: "Liens externes",
        icon: "i-heroicons-link",
        to: `${basePath}/external-links`,
      },
      ...(qandaActive.value
        ? [
            {
              label: "Scanzee",
              icon: "i-heroicons-qr-code",
              to: `${basePath}/scanzee`,
            },
          ]
        : []),
    ];
  });

  return { sponsorLinks };
};
