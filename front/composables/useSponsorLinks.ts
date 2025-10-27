export const useSponsorLinks = (orgSlug: string, eventSlug: string, sponsorId: string) => {
  const sponsorLinks = [
    {
      label: 'Retour aux sponsors',
      icon: 'i-heroicons-arrow-left',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors`
    },
    {
      label: 'Partenariat',
      icon: 'i-heroicons-hand-raised',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}`
    },
    {
      label: 'Tickets',
      icon: 'i-heroicons-ticket',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/tickets`
    },
    {
      label: 'Communication',
      icon: 'i-heroicons-megaphone',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/communication`
    },
    {
      label: 'Documents',
      icon: 'i-heroicons-document-text',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/documents`
    },
    {
      label: 'Entreprise',
      icon: 'i-heroicons-building-office',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsorId}/company`
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
