export const useDashboardLinks = () => {
  const { logoutLink } = useLogoutLink()

  const mainLinks = [{
    label: 'Mes organisations',
    icon: 'i-heroicons-building-office',
    to: '/orgs'
  }, {
    label: 'Créer une organisation',
    icon: 'i-heroicons-plus-circle',
    to: '/orgs/create'
  }]

  const footerLinks = [logoutLink]

  return {
    mainLinks,
    footerLinks
  }
}
