interface PartnerLinksOptions {
  orgSlug: string;
  eventSlug: string;
  partnerId: string;
}

export const usePartnerLinks = (
  orgSlugOrOptions: string | PartnerLinksOptions,
  eventSlug?: string,
  partnerId?: string,
) => {
  const options: PartnerLinksOptions =
    typeof orgSlugOrOptions === "string"
      ? {
          orgSlug: orgSlugOrOptions,
          eventSlug: eventSlug!,
          partnerId: partnerId!,
        }
      : orgSlugOrOptions;

  const partnerLinks = computed(() => {
    const basePath = `/orgs/${options.orgSlug}/events/${options.eventSlug}/partners/${options.partnerId}`;

    return [
      {
        label: "Retour aux partenaires",
        icon: "i-heroicons-arrow-left",
        to: `/orgs/${options.orgSlug}/events/${options.eventSlug}/partners`,
      },
      {
        label: "Partenariat",
        icon: "i-heroicons-hand-raised",
        to: basePath,
      },
      {
        label: "Entité légale",
        icon: "i-heroicons-building-office",
        to: `${basePath}/legal-entity`,
      },
      {
        label: "Liens externes",
        icon: "i-heroicons-link",
        to: `${basePath}/external-links`,
      },
      {
        label: "Tickets",
        icon: "i-heroicons-ticket",
        to: basePath,
        disabled: true,
        badge: {
          label: "Bientôt",
          color: "neutral" as const,
          title: "Disponible prochainement",
        },
      },
      {
        label: "Communication",
        icon: "i-heroicons-megaphone",
        to: basePath,
        disabled: true,
        badge: {
          label: "Bientôt",
          color: "neutral" as const,
          title: "Disponible prochainement",
        },
      },
    ];
  });

  return { partnerLinks };
};
