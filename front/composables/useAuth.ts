import { ref, computed } from 'vue'
import type { UserSession, UserInfo } from '~/utils/api'

interface AuthState {
  user: UserInfo | null
  token: string | null
  isAuthenticated: boolean
}

const authState = ref<AuthState>({
  user: null,
  token: null,
  isAuthenticated: false
})

export const useAuth = () => {
  const isAuthenticated = computed(() => authState.value.isAuthenticated)
  const user = computed(() => authState.value.user)
  const token = computed(() => authState.value.token)

  const login = () => {
    const config = useRuntimeConfig()
    const redirectUrl = encodeURIComponent(`${window.location.origin}/auth/callback`)
    window.location.href = `${config.public.apiBaseUrl}/auth/login?redirectUrl=${redirectUrl}`
  }

  const logout = async () => {
    authState.value = {
      user: null,
      token: null,
      isAuthenticated: false
    }
    
    // Clear token from localStorage
    if (import.meta.client) {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('user_info')
    }
    
    // Redirect to home page
    await navigateTo('/')
  }

  const handleAuthCallback = async (sessionData: UserSession) => {
    authState.value.token = sessionData.token
    authState.value.isAuthenticated = true
    
    // Store token in localStorage for persistence
    if (import.meta.client) {
      localStorage.setItem('auth_token', sessionData.token)
    }
    
    // You might want to fetch user info here or decode from token
    // For now, we'll set a placeholder
    authState.value.user = {
      displayName: 'User', // This should come from your API
      email: '',
      pictureUrl: null
    }
  }

  const initializeAuth = () => {
    if (import.meta.client) {
      const storedToken = localStorage.getItem('auth_token')
      const storedUser = localStorage.getItem('user_info')
      
      if (storedToken) {
        authState.value.token = storedToken
        authState.value.isAuthenticated = true
        
        if (storedUser) {
          authState.value.user = JSON.parse(storedUser)
        }
      }
    }
  }

  return {
    isAuthenticated,
    user,
    token,
    login,
    logout,
    handleAuthCallback,
    initializeAuth
  }
}