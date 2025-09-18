<template>
  <div>
    <p v-if="loading">Connexion en cours...</p>
    <p v-else-if="error">Erreur lors de la connexion: {{ error }}</p>
  </div>
</template>

<script setup lang="ts">
const { handleAuthCallback } = useAuth()
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const route = useRoute()
    const token = route.query.token as string
    
    if (token) {
      // Create session data with the token from URL
      const sessionData = {
        token: token,
        state: 'authenticated'
      }
      
      await handleAuthCallback(sessionData)
      console.log(route.query)

      // Redirect to organizations page
      await navigateTo('/orgs')
    } else {
      throw new Error('No token received in URL')
    }
  } catch (err) {
    console.error('Auth callback error:', err)
    error.value = err instanceof Error ? err.message : 'Unknown error'
    
    // Redirect to home page after error
    setTimeout(() => {
      navigateTo('/')
    }, 3000)
  } finally {
    loading.value = false
  }
})

useHead({
  title: 'Connexion | DevLille'
})
</script>