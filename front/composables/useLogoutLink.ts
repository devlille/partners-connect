export const useLogoutLink = () => {
  const logoutLink = {
    label: 'Déconnexion',
    icon: 'i-heroicons-arrow-right-on-rectangle',
    click: () => {
      navigateTo('/api/auth/signout', { external: true })
    }
  }

  return { logoutLink }
}
