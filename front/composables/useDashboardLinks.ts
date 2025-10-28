export const useDashboardLinks = () => {
  const mainLinks = [{
    label: 'Mes organisations',
    icon: 'i-heroicons-building-office',
    to: '/orgs'
  }, {
    label: 'Événements Favoris',
    icon: 'i-heroicons-star',
    to: '/orgs/favorites'
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
      // Supprimer le token d'authentification
      if (import.meta.client) {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('user_info');
      }
      // Rediriger vers la page de login
      navigateTo('/login')
    }
  }]

  return {
    mainLinks,
    footerLinks
  }
}
