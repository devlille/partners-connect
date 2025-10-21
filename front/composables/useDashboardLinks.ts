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
    label: 'Paramètres',
    icon: 'i-heroicons-cog-6-tooth',
    to: '/settings'
  }, {
    label: 'Aide',
    icon: 'i-heroicons-question-mark-circle',
    to: '/help'
  }]

  return {
    mainLinks,
    footerLinks
  }
}
