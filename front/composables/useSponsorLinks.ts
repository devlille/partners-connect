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
    },
    {
      label: 'Offres d\'emploi',
      icon: 'i-heroicons-briefcase',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/job-offers`
    },
    {
      label: 'Liens externes',
      icon: 'i-heroicons-link',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/external-links`
    }
  ]

  return { sponsorLinks }
}
