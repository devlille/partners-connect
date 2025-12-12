export const useOrgLinks = (orgSlug: string) => {
  const orgLinks = [
    {
      label: "Informations",
      icon: "i-heroicons-information-circle",
      to: `/orgs/${orgSlug}`,
    },
    {
      label: "Événements",
      icon: "i-heroicons-calendar",
      to: `/orgs/${orgSlug}/events`,
    },
    {
      label: "Utilisateurs",
      icon: "i-heroicons-users",
      to: `/orgs/${orgSlug}/users`,
    },
  ];

  return { orgLinks };
};
