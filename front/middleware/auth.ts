export default defineNuxtRouteMiddleware((to) => {
  // Skip auth check if page explicitly sets auth: false
  if (to.meta.auth === false) {
    return;
  }

  // Only check auth on client side
  if (import.meta.client) {
    const { isAuthenticated, initializeAuth } = useAuth();

    // Initialize auth state from localStorage
    initializeAuth();

    // Simple check: if no token in localStorage, redirect
    if (!isAuthenticated.value) {
      const config = useRuntimeConfig();
      const redirectUrl = encodeURIComponent(
        `${process.env.NODE_ENV === "development" ? "http://localhost:8080" : window.location.origin}${to.fullPath}`
      );
      window.location.href = `${config.public.apiBaseUrl}/auth/login?redirectUrl=${redirectUrl}`;
    }
  }
})