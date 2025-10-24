export const useSponsorLinks = (orgSlug: string, eventSlug: string, sponsorId: string) => {
  const sponsorLinks = [
    {
      label: 'Retour aux sponsors',
      icon: 'i-heroicons-arrow-left',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors`
    },
    {
      label: 'Informations',
      icon: 'i-heroicons-information-circle',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}`
    },
    {
      label: 'Documents',
      icon: 'i-heroicons-document',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/documents`
    }
  ]

  return { sponsorLinks }
}
