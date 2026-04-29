export const useEventLinks = (orgSlug: string, eventSlug: string) => {
  const eventLinks = computed(() => {
    const links = [
      {
        label: "Dashboard",
        icon: "i-heroicons-chart-bar",
        to: `/orgs/${orgSlug}/events/${eventSlug}/dashboard`,
      },
      {
        label: "Informations",
        icon: "i-heroicons-information-circle",
        to: `/orgs/${orgSlug}/events/${eventSlug}`,
      },
      {
        label: "Mes Packs",
        icon: "i-heroicons-cube",
        to: `/orgs/${orgSlug}/events/${eventSlug}/packs`,
      },
      {
        label: "Options",
        icon: "i-heroicons-adjustments-horizontal",
        to: `/orgs/${orgSlug}/events/${eventSlug}/options`,
      },
      {
        label: "Sponsors",
        icon: "i-heroicons-building-office-2",
        to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors`,
      },
      {
        label: "Offres d'emploi",
        icon: "i-heroicons-briefcase",
        to: `/orgs/${orgSlug}/events/${eventSlug}/job-offers`,
      },
      {
        label: "Prestataires",
        icon: "i-heroicons-user-group",
        to: `/orgs/${orgSlug}/events/${eventSlug}/providers`,
      },
      {
        label: "Liens externes",
        icon: "i-heroicons-link",
        to: `/orgs/${orgSlug}/events/${eventSlug}/external-links`,
      },
      {
        label: "Communication",
        icon: "i-heroicons-megaphone",
        to: `/orgs/${orgSlug}/events/${eventSlug}/communication`,
      },
      {
        label: "Intégrations",
        icon: "i-heroicons-puzzle-piece",
        to: `/orgs/${orgSlug}/events/${eventSlug}/integrations`,
      },
      {
        label: "Plan des Booths",
        icon: "i-heroicons-map",
        to: `/orgs/${orgSlug}/events/${eventSlug}/booth-plan`,
      },
      {
        label: "Agenda",
        icon: "i-heroicons-calendar-days",
        to: `/orgs/${orgSlug}/events/${eventSlug}/agenda`,
      },
    ];

    return links;
  });

  return {
    eventLinks,
  };
};
