export default defineNuxtRouteMiddleware((to) => {
  // Only check auth on client side
  if (import.meta.client) {
    const { isAuthenticated, initializeAuth } = useAuth()
    
    // Initialize auth state from localStorage
    initializeAuth()
    
    // Simple check: if no token in localStorage, redirect
    if (!isAuthenticated.value) {
      const config = useRuntimeConfig()
      const redirectUrl = encodeURIComponent(`${process.env.NODE_ENV === 'development' ? 'http://localhost:3000' : window.location.origin}${to.fullPath}`)
      window.location.href = `${config.public.apiBaseUrl}/auth/login?redirectUrl=${redirectUrl}`
    }
  }
})