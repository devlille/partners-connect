export const useEventLinks = (orgSlug: string, eventSlug: string) => {
  const eventLinks = [
    {
      label: 'Informations',
      icon: 'i-heroicons-information-circle',
      to: `/orgs/${orgSlug}/events/${eventSlug}`
    },
    {
      label: 'Mes Packs',
      icon: 'i-heroicons-cube',
      to: `/orgs/${orgSlug}/events/${eventSlug}/packs`
    },
    {
      label: 'Options',
      icon: 'i-heroicons-adjustments-horizontal',
      to: `/orgs/${orgSlug}/events/${eventSlug}/options`
    },
    {
      label: 'Sponsors',
      icon: 'i-heroicons-building-office-2',
      to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors`
    },
    {
      label: 'Prestataires',
      icon: 'i-heroicons-user-group',
      to: `/orgs/${orgSlug}/events/${eventSlug}/providers`
    },
    {
      label: 'Liens externes',
      icon: 'i-heroicons-link',
      to: `/orgs/${orgSlug}/events/${eventSlug}/external-links`
    }
  ]

  return {
    eventLinks
  }
}
