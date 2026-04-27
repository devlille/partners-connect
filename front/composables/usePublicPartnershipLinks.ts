export const usePublicPartnershipLinks = () => {
  const { eventSlug, partnershipId, partnership, company, qandaEnabled } = usePublicPartnership();
  const { isPartnershipComplete, isCompanyComplete } = usePartnershipValidation();

  const sidebarLinks = computed(() => {
    const partnershipComplete = isPartnershipComplete(partnership.value);
    const companyComplete = isCompanyComplete(company.value);

    return [
      {
        label: 'Partenariat',
        icon: 'i-heroicons-hand-raised',
        to: `/${eventSlug.value}/${partnershipId.value}`,
        badge: !partnershipComplete
          ? { label: '!', color: 'error' as const, title: 'Informations incomplètes' }
          : undefined,
      },
      {
        label: 'Entreprise',
        icon: 'i-heroicons-building-office',
        to: `/${eventSlug.value}/${partnershipId.value}/company`,
        badge: !companyComplete
          ? { label: '!', color: 'error' as const, title: 'Informations incomplètes' }
          : undefined,
      },
      {
        label: "Offres d'emploi",
        icon: 'i-heroicons-briefcase',
        to: `/${eventSlug.value}/${partnershipId.value}/job-offers`,
      },
      {
        label: 'Vos Activités',
        icon: 'i-heroicons-calendar-days',
        to: `/${eventSlug.value}/${partnershipId.value}/activities`,
      },
      {
        label: 'Liens utiles',
        icon: 'i-heroicons-link',
        to: `/${eventSlug.value}/${partnershipId.value}/external-links`,
      },
      {
        label: 'Prestataires',
        icon: 'i-heroicons-user-group',
        to: `/${eventSlug.value}/${partnershipId.value}/providers`,
      },
      {
        label: 'Booth',
        icon: 'i-heroicons-map-pin',
        to: `/${eventSlug.value}/${partnershipId.value}/booth`,
      },
      {
        label: 'Tickets',
        icon: 'i-heroicons-ticket',
        to: `/${eventSlug.value}/${partnershipId.value}/tickets`,
      },
      ...(qandaEnabled.value ? [{
        label: 'Scanzee',
        icon: 'i-heroicons-qr-code',
        to: `/${eventSlug.value}/${partnershipId.value}/scanzee`,
      }] : []),
    ];
  });

  return { sidebarLinks };
};
