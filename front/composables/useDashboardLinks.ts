export const useDashboardLinks = () => {
  const mainLinks = [{
    label: 'Mes organisations',
    icon: 'i-heroicons-building-office',
    to: '/orgs'
  }, {
    label: 'Créer une organisation',
    icon: 'i-heroicons-plus-circle',
    to: '/orgs/create'
  }]

  const footerLinks = [{
    label: 'Déconnexion',
    icon: 'i-heroicons-arrow-right-on-rectangle',
    to: '#',
    click: () => {
      navigateTo('/api/auth/signout', { external: true })
    }
  }]

  return {
    mainLinks,
    footerLinks
  }
}
